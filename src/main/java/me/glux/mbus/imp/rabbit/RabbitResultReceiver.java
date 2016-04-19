package me.glux.mbus.imp.rabbit;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

import me.glux.mbus.ResultReceiver;
import me.glux.mbus.asyn.ParkedObject;
import me.glux.mbus.asyn.TimeoutObjectManager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitResultReceiver extends ResultReceiver {
	private static final Logger logger = LoggerFactory.getLogger(RabbitResultReceiver.class);
	private TimeoutObjectManager<? extends ParkedObject> callers;
	private BlockingQueue<byte[]> cache = new LinkedBlockingQueue<byte[]>(1);

	public RabbitResultReceiver(Channel channel, String clientId,String resultExchange, TimeoutObjectManager<? extends ParkedObject> callers) {
		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				logger.debug("Received mq result:\n"+new String(body));
				try {
					do {
					} while (!cache.offer(body, 5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					logger.warn("Put result into local cache error.", e);
				}
			}
		};
		try {
			channel.basicQos(1);
			DeclareOk result=channel.queueDeclare(clientId, false, false, true, null);
			channel.queueBind(result.getQueue(), resultExchange, clientId);
			channel.basicConsume(result.getQueue(), true, consumer);
		} catch (IOException e) {
			throw new IllegalStateException("Configure rabbit mq error.",e);
		}
		this.callers = callers;
		super.start();
	}
	
	@Override
	protected byte[] receive() {
		try {
			do {
				byte[] result;
				result = cache.poll(5, TimeUnit.SECONDS);
				if (null != result) {
					logger.debug("Received result form local cache:\n"+new String(result));
					return result;
				}
			} while (true);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Error while reading result from local cache.", e);
		}
	}

	@Override
	protected void notifyRunner(String id) {
		ParkedObject object = callers.get(id);
		if (null != object) {
			object.done();
		}
	}

}
