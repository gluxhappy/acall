package me.glux.mbus.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import org.springframework.jmx.access.InvalidInvocationException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import me.glux.mbus.RequestSender;
import me.glux.mbus.ResultReceiver;
import me.glux.mbus.annotation.MbusApi;
import me.glux.mbus.annotation.MbusService;
import me.glux.mbus.asyn.ParkedThread;
import me.glux.mbus.asyn.TimeoutObjectManager;
import me.glux.mbus.protocal.ExceptionHolder;
import me.glux.mbus.protocal.RequestEntity;
import me.glux.mbus.protocal.ResponseEntity;
import me.glux.mbus.protocal.ResponseStatus;

public class AsynProxyClient implements InvocationHandler {
    private static final long DEFAULT_TIMEOUT = 5 * 1000L;
    private long timeout = DEFAULT_TIMEOUT;
    private String clientId = "client-" + UUID.randomUUID();
    private RequestSender sender;
    private ResultReceiver receiver;
    private ObjectMapper mapper = new ObjectMapper();
    private TimeoutObjectManager<ParkedThread> parkedTheadPool;

    public AsynProxyClient(String clientId,RequestSender sender,ResultReceiver receiver,TimeoutObjectManager<ParkedThread> parkedTheadPool) {
        this.clientId=clientId;
        this.sender=sender;
        this.receiver=receiver;
        this.parkedTheadPool = parkedTheadPool;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RequestEntity entity = new RequestEntity();
        // set id
        entity.setId(UUID.randomUUID().toString());
        // set target
        entity.setTarget(parseTarget(method));
        // set content
        try {
            entity.setContent(mapper.writer().writeValueAsString(args));
        } catch (Exception e) {
            throw new IllegalStateException("Parameter(s) can not be serialize.",e);
        }
        entity.setRequester(clientId);
        sender.send(entity);
        ParkedThread parkedThread = new ParkedThread();
        parkedTheadPool.put(entity.getId(), timeout, parkedThread);
        parkedThread.park(timeout);
        parkedTheadPool.get(entity.getId());
        ResponseEntity response = receiver.get(entity.getId());
        if (null == response) {
            throw new IllegalStateException("Illegal response.");
        }
        if (response.getStatus() == ResponseStatus.ERROR) {
            ExceptionHolder exceptionHolder = mapper.readValue(response.getResult(), ExceptionHolder.class);
            throw new InvalidInvocationException(
                    "Remote call [" + exceptionHolder.getType() + "]:" + exceptionHolder.getMessage());
        } else {
            if(void.class.equals(method.getReturnType())){
                return null;
            }else{
                JavaType paramJavaType = TypeFactory.defaultInstance().constructType(method.getGenericReturnType());
                return mapper.readValue(response.getResult(), paramJavaType);
            }
        }
    }

    private static String parseTarget(Method method) {
        MbusService serviceAnno = method.getDeclaringClass().getAnnotation(MbusService.class);
        if (null == serviceAnno) {
            throw new IllegalStateException(
                    "Interface of method[" + method.getName() + "] is not annotated as @MbusService");
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(serviceAnno.value());// module name
        sb.append('.');
        MbusApi apiAnno = method.getAnnotation(MbusApi.class);
        if (apiAnno == null) {
            throw new IllegalStateException("Method[" + method.getName() + "] is not annotated as @MbusApi");
        }
        sb.append(apiAnno.value());
        return sb.toString();
    }
}
