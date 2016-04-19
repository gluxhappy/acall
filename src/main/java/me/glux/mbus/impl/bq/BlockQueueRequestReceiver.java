package me.glux.mbus.impl.bq;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.glux.mbus.RequestReceiver;
import me.glux.mbus.protocal.RequestEntity;

public class BlockQueueRequestReceiver implements RequestReceiver {
    private static final Logger logger = LoggerFactory.getLogger(BlockQueueRequestReceiver.class);
    private ObjectMapper mapper = new ObjectMapper();
    private BlockingQueue<String> queue ;
    
    public BlockQueueRequestReceiver(BlockingQueue<String> queue){
        this.queue=queue;
    }
    
    @Override
    public RequestEntity receive() throws InterruptedException {
        do {
            String requestStr = queue.poll(5, TimeUnit.SECONDS);
            if(null != requestStr){
                try {
                    return mapper.readValue(requestStr, RequestEntity.class);
                } catch (IOException e) {
                    logger.warn("Invalid request received:\n" + requestStr, e);
                    continue;
                }
            }
        } while (true);
    }

    @Override
    public void registerApi(String api) {
        // 基于内存队列的实现，不需要注册api到服务器
    }

    @Override
    public void registerServer(String server) {
        // 基于内存队列的实现，不需要注册api到服务器
    }
}
