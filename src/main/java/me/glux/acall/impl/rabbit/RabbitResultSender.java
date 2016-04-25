package me.glux.acall.impl.rabbit;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import me.glux.acall.ResultSender;
import me.glux.acall.protocal.ResponseEntity;

public class RabbitResultSender implements ResultSender {
	private static final Logger logger=LoggerFactory.getLogger(RabbitResultSender.class);
	private Channel channel;
	private String requestExchange;
	private ObjectMapper mapper = new ObjectMapper();
	public RabbitResultSender(Channel channel,String requestExchange){
		this.channel=channel;
		this.requestExchange=requestExchange;
	}
	@Override
	public void send(ResponseEntity response) {
		if (null == response) {
			throw new IllegalArgumentException("Trying to send null result entity.");
		}
		byte[] sendEntity = null;
		try {
			sendEntity = mapper.writer().writeValueAsBytes(response);
			logger.debug("Sending result:\n"+new String(sendEntity));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Serialize result entity error.", e);
		}
		try {
			channel.basicPublish(requestExchange, response.getRequester(), null, sendEntity);
		} catch (IOException e) {
			throw new IllegalStateException("Send entity error.", e);
		}
	}

}
