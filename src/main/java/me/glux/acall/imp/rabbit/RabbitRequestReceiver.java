package me.glux.acall.imp.rabbit;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import me.glux.acall.RequestReceiver;
import me.glux.acall.protocal.RequestEntity;

public class RabbitRequestReceiver implements RequestReceiver {
    private static final Logger logger = LoggerFactory.getLogger(RabbitRequestReceiver.class);
    private Channel channel;
    private String requestExchange;
    private BlockingQueue<RequestEntity> cache = new LinkedBlockingQueue<RequestEntity>(1);
    private ObjectMapper mapper = new ObjectMapper();
    private Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            logger.debug("Received request:\n" + new String(body));
            RequestEntity request = null;
            try {
                request = mapper.readValue(body, RequestEntity.class);
            } catch (Exception e) {
                logger.warn("Received invalid request.", e);
            }

            if (null == request) {
                return;
            }
            try {
                while (true) {
                    if (cache.offer(request, 5, TimeUnit.SECONDS)) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Put request into local cache error.", e);
            }
        }

    };

    public RabbitRequestReceiver(Channel channel, String requestExchange) {
        this.channel = channel;
        this.requestExchange = requestExchange;

        try {
            channel.basicQos(1);
        } catch (IOException e) {
            throw new IllegalStateException("Configure rabbit mq error.", e);
        }
    }

    @Override
    public void registerServer(String server) {
        // 当前实现不急于集群管理服务器，不需要注册服务器
    }

    @Override
    public void registerApi(String api) {
        try {
            String apiQueue = "api-" + api;
            channel.queueDeclare(apiQueue, false, false, true, null);
            channel.queueBind(apiQueue, requestExchange, api);
            channel.basicConsume(apiQueue, true, consumer);
        } catch (IOException e) {
            throw new IllegalStateException("Error while register api.", e);
        }
    }

    @Override
    public RequestEntity receive() throws InterruptedException {
        do {
            RequestEntity request = cache.poll(5, TimeUnit.SECONDS);
            if (null != request) {
                return request;
            }
        } while (true);
    }

}
