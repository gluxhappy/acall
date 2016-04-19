package me.glux.acall.test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;

import me.glux.acall.RequestReceiver;
import me.glux.acall.RequestSender;
import me.glux.acall.ResultReceiver;
import me.glux.acall.ResultSender;
import me.glux.acall.asyn.ParkedThread;
import me.glux.acall.asyn.TimeoutObjectManager;
import me.glux.acall.client.AsynProxyClient;
import me.glux.acall.export.StaticExportor;
import me.glux.acall.imp.rabbit.RabbitRequestReceiver;
import me.glux.acall.imp.rabbit.RabbitRequestSender;
import me.glux.acall.imp.rabbit.RabbitResultReceiver;
import me.glux.acall.imp.rabbit.RabbitResultSender;
import me.glux.acall.impl.bq.BlockQueueRequestReceiver;
import me.glux.acall.impl.bq.BlockQueueRequestSender;
import me.glux.acall.impl.bq.BlockQueueResultReceiver;
import me.glux.acall.impl.bq.BlockQueueResultSender;
import me.glux.acall.server.AsynProxyServer;

public class DemoTest {
	private static final Logger logger = LoggerFactory.getLogger(DemoTest.class);

	@Test
	public void testDemo() {
		try {
			String requestExchange = "request";
			String resultExchange = "result";
			String serverName = "test_server";
			String clientId = "test_client";
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("192.168.1.17");
			factory.setVirtualHost("asyn");
			factory.setUsername("asyn");
			factory.setPassword("asyn");

			RequestSender requestSender = new RabbitRequestSender(factory.newConnection().createChannel(),
					requestExchange);
			RequestReceiver requestResceiver = new RabbitRequestReceiver(factory.newConnection().createChannel(),
					requestExchange);
			requestResceiver.registerServer(serverName);
			ResultSender resultSender = new RabbitResultSender(factory.newConnection().createChannel(), resultExchange);
			TimeoutObjectManager<ParkedThread> parkedTheadPool = new TimeoutObjectManager<ParkedThread>();
			ResultReceiver resultReceiver = new RabbitResultReceiver(factory.newConnection().createChannel(), clientId,
					resultExchange, parkedTheadPool);
			resultReceiver.setResultHoldTime(5 * 1000);
			AsynProxyServer server = new AsynProxyServer(serverName, resultSender, requestResceiver);
			server.registerInterface(Demo.class, new DemoImpl());
			AsynProxyClient client = new AsynProxyClient(clientId,requestSender, resultReceiver, parkedTheadPool);
			Demo demoHander = StaticExportor.createProxy(client, Demo.class);
			server.start();
			String callResult=demoHander.call("test");
			logger.debug("Call Result:" +callResult);
		} catch (Exception e) {
			logger.warn("Error:", e);
			throw new IllegalStateException("Error", e);
		}
	}

	@Test
	public void testDemoBq() {
		try {
			String serverName = "default_server";
			String clientId="test_client";
			BlockingQueue<String> requestQueue = new LinkedBlockingQueue<String>(100);
			BlockingQueue<String> resulteQueue = new LinkedBlockingQueue<String>(100);
			RequestSender requestSender = new BlockQueueRequestSender(requestQueue);
			RequestReceiver requestResceiver = new BlockQueueRequestReceiver(requestQueue);
			ResultSender resultSender = new BlockQueueResultSender(resulteQueue);
			TimeoutObjectManager<ParkedThread> parkedTheadPool = new TimeoutObjectManager<ParkedThread>();
			ResultReceiver resultReceiver = new BlockQueueResultReceiver(resulteQueue, parkedTheadPool);
			resultReceiver.setResultHoldTime(5 * 1000);
			AsynProxyServer server = new AsynProxyServer(serverName, resultSender, requestResceiver);
			server.registerInterface(Demo.class, new DemoImpl());
			AsynProxyClient client = new AsynProxyClient(clientId,requestSender, resultReceiver, parkedTheadPool);
			Demo demoHander = StaticExportor.createProxy(client, Demo.class);
			server.start();
			logger.debug("Call Result:" + demoHander.call("test"));
			System.currentTimeMillis();
		} catch (Exception e) {
			logger.warn("Error:", e);
			throw e;
		}
	}
}
