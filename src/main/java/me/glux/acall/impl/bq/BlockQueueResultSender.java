package me.glux.acall.impl.bq;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.glux.acall.ResultSender;
import me.glux.acall.protocal.ResponseEntity;

public class BlockQueueResultSender implements ResultSender {
    private ObjectMapper mapper = new ObjectMapper();
    private BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
    
    public BlockQueueResultSender(BlockingQueue<String> queue){
        this.queue=queue;
    }

    @Override
    public void send(ResponseEntity response) {
        try {
            queue.offer(mapper.writer().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialize response error.",e);
        }
    }

}
