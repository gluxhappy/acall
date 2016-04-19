package me.glux.acall.imp.rabbit;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import me.glux.acall.RequestSender;
import me.glux.acall.protocal.RequestEntity;

public class RabbitRequestSender implements RequestSender {
	private static final Logger logger=LoggerFactory.getLogger(RabbitRequestSender.class);
	private Channel channel;
	private String requestExchange;
	private ObjectMapper mapper = new ObjectMapper();
	
	public RabbitRequestSender(Channel channel,String requestExchange){
		this.channel=channel;
		this.requestExchange=requestExchange;
	}
	@Override
	public void send(RequestEntity request) {
		if (null == request) {
			throw new IllegalArgumentException("Trying to send null request entity.");
		}
		byte[] sendEntity = null;
		try {
			sendEntity = mapper.writer().writeValueAsBytes(request);
			logger.debug("Sending request:\n"+new String(sendEntity));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Serialize request entity error.", e);
		}
		try {
			channel.basicPublish(requestExchange, request.getTarget(), null, sendEntity);
		} catch (IOException e) {
			throw new IllegalStateException("Send entity error.", e);
		}
	}

}
