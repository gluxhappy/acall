package me.glux.acall.impl.bq;

import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.glux.acall.RequestSender;
import me.glux.acall.protocal.RequestEntity;

public class BlockQueueRequestSender implements RequestSender {
	private ObjectMapper mapper = new ObjectMapper();
	private BlockingQueue<String> queue;

	public BlockQueueRequestSender(BlockingQueue<String> queue) {
		this.queue = queue;
	}

	@Override
	public void send(RequestEntity request) {
		try {
			queue.offer(mapper.writer().writeValueAsString(request));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Error serialize reqeust entity.", e);
		}
	}

}
