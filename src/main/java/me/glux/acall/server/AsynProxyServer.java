package me.glux.acall.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import me.glux.acall.RequestReceiver;
import me.glux.acall.ResultSender;
import me.glux.acall.annotation.MbusApi;
import me.glux.acall.annotation.MbusService;
import me.glux.acall.client.ProxyInterface;
import me.glux.acall.model.Api;
import me.glux.acall.protocal.ExceptionHolder;
import me.glux.acall.protocal.RequestEntity;
import me.glux.acall.protocal.ResponseEntity;
import me.glux.acall.protocal.ResponseStatus;

public class AsynProxyServer {
    private static final Logger logger = LoggerFactory.getLogger(AsynProxyServer.class);
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_ERROR_OBJECT = "{\"status\":\"ERROR\",\"message\":\"Internal error.\"}";

    private ExecutorService pool = null;
    private ObjectMapper mapper = DEFAULT_MAPPER;
    private String clientId = "Default_client_id";
    private Map<String, Api> apis = new HashMap<>(128);

    private ResultSender resultSender;
    private RequestReceiver requestReceiver;

    public AsynProxyServer(String serverName, ResultSender resultSender, RequestReceiver requestReceiver) {
        this.clientId = serverName;
        this.resultSender = resultSender;
        this.requestReceiver = requestReceiver;
    }

    public void registerInterface(Class<?> interfaze, Object handler) {
        if (ProxyInterface.class.isInstance(handler)) {
            throw new IllegalArgumentException("Can not create endpoint for proxyed object");
        }
        if (!interfaze.isInstance(handler)) {
            throw new IllegalArgumentException("Handler does not implemnt target interface.");
        }
        MbusService anno = interfaze.getAnnotation(MbusService.class);
        if (null == anno) {
            throw new IllegalArgumentException("Interfcae not annotated with @MbusService");
        }
        for (Method m : interfaze.getMethods()) {
            MbusApi apiAnno = m.getAnnotation(MbusApi.class);
            if (apiAnno == null) {
                logger.warn("Method[" + m.getName() + "] of interface[" + interfaze.getName()
                        + "] declared as MbusService but not MbusApi.");
            }
            String type = anno.value() + '.' + apiAnno.value();
            registerApi(type, m, handler);
        }
    }

    public void start() {
        Thread requestReceiveThread = new Thread(new ReceiveRunner());
        requestReceiveThread.start();
        pool = new ThreadPoolExecutor(2, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    private void registerApi(String type, Method m, Object handler) {
        if (apis.containsKey(type)) {
            Api old = apis.get(type);
            throw new IllegalStateException("Trying to register [" + type + "] for method ["
                    + m.getDeclaringClass().getName() + '.' + m.getName() + ", but it has already be used by method["
                    + old.getMethod().getDeclaringClass().getName() + '.' + old.getMethod().getName());
        }
        requestReceiver.registerApi(type);
        apis.put(type, new Api(m, handler));
    }

    private class ReceiveRunner implements Runnable {
        @Override
        public void run() {
            do {
                try {
                    RequestEntity request = requestReceiver.receive();
                    InvokeEntity invokeEntity = new InvokeEntity(request);
                    pool.submit(new CallRunner(invokeEntity));
                } catch (InterruptedException e) {
                    logger.warn("Error while waitting request.", e);
                } catch (Exception e) {
                    logger.warn("Error while processing received request.", e);
                }
            } while (true);
        }
    }

    private class CallRunner implements Runnable {
        private InvokeEntity invokeEntity;

        public CallRunner(InvokeEntity invokeEntity) {
            this.invokeEntity = invokeEntity;
        }

        @Override
        public void run() {
            try {
                call();
            } catch (Exception e) {
                logger.error("Error while invoke request " + invokeEntity.getRequest().getId(), e);
            }
        }

        private void call() {
            ResponseEntity response = invokeEntity.getResponse();
            response.setAnswerer(clientId);
            response.setId(invokeEntity.getRequest().getId());
            response.setRequester(invokeEntity.getRequest().getRequester());
            resultSender.send(response);
        }
    }

    private class InvokeEntity {
        private Api api;
        private RequestEntity request;

        public InvokeEntity(RequestEntity request) {
            if (null != request && null != request.getTarget()) {
                this.api = apis.get(request.getTarget());
            }
            this.request = request;
        }

        public RequestEntity getRequest() {
            return request;
        }

        public ResponseEntity getResponse() {
            try {
                return invoke();
            } catch (JsonProcessingException e) { // NOSONAR
                ResponseEntity internalError = new ResponseEntity();
                internalError.setStatus(ResponseStatus.ERROR);
                internalError.setResult(DEFAULT_ERROR_OBJECT);
                return internalError;
            }
        }

        private ResponseEntity invoke() throws JsonProcessingException {
            ResponseEntity callResult = new ResponseEntity();
            if (null == api) {
                callResult.setStatus(ResponseStatus.ERROR);
                ExceptionHolder exceptionHolder = new ExceptionHolder(IllegalStateException.class.getName(),
                        "Target api not found.");
                callResult.setResult(mapper.writer().writeValueAsString(exceptionHolder));
                return callResult;
            }
            Object[] args;
            try {
                args = parseArgs();
            } catch (Throwable e) { // NOSONAR
                callResult.setStatus(ResponseStatus.ERROR);
                ExceptionHolder exceptionHolder = new ExceptionHolder(e.getClass().getName(), e.getMessage());
                callResult.setResult(mapper.writer().writeValueAsString(exceptionHolder));
                return callResult;
            }

            try {
                String resultStr=null;
                if(void.class.equals(api.getMethod().getReturnType())){
                    api.getMethod().invoke(api.getHandler(), args);
                    resultStr="null";
                }else{
                    Object resultObj = api.getMethod().invoke(api.getHandler(), args);
                    resultStr=mapper.writer().writeValueAsString(resultObj);
                }
                callResult.setStatus(ResponseStatus.SUCCESS);
                callResult.setResult(resultStr);
            } catch (Exception e) {
                callResult.setStatus(ResponseStatus.ERROR);
                ExceptionHolder exceptionHolder ;
                if (InvocationTargetException.class.isInstance(e)) {
                    Throwable ex = (InvocationTargetException.class.cast(e)).getTargetException();
                    exceptionHolder = new ExceptionHolder(ex.getClass().getName(), ex.getMessage());
                } else {
                    if (IllegalAccessException.class.isInstance(e)) {
                        exceptionHolder = new ExceptionHolder(e.getClass().getName(),
                                "Api is protected,contact provider.");
                    } else if (IllegalArgumentException.class.isInstance(e)) {
                        exceptionHolder = new ExceptionHolder(e.getClass().getName(),
                                "Provided arguments is not suitable to target api.");
                    } else {
                        exceptionHolder = new ExceptionHolder(e.getClass().getName(),
                                "Api is protected,contact provider.");
                    }
                }
                callResult.setResult(mapper.writer().writeValueAsString(exceptionHolder));
            }
            return callResult;
        }

        private Object[] parseArgs() {
            Method m = api.getMethod();
            Type[] paramTypes = m.getGenericParameterTypes();
            if (paramTypes.length == 0) {
                return new Object[] {};
            }

            JsonNode paramsNode;
            try {
                paramsNode = mapper.readTree(request.getContent());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Provided request parameter(s) is no valid json.", e);
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading input request.", e);
            }
            if (!paramsNode.isArray()) {
                throw new IllegalArgumentException("Only support call with array paramter(s).");
            }
            ArrayNode arrayParamsNode = ArrayNode.class.cast(paramsNode);
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    JsonParser paramJsonParser = mapper.treeAsTokens(arrayParamsNode.get(i));
                    JavaType paramJavaType = TypeFactory.defaultInstance().constructType(paramTypes[i]);
                    args[i] = mapper.readValue(paramJsonParser, paramJavaType);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException(
                            "Error while parsing parameter No." + (i + 1) + ", type not match.", e);
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Error while parsing parameter No." + (i + 1) + ", error reading input.", e);
                }
            }
            return args;
        }
    }
}
