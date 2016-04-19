package me.glux.acall.export;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import me.glux.acall.RequestReceiver;
import me.glux.acall.ResultSender;
import me.glux.acall.annotation.MbusService;
import me.glux.acall.client.ProxyInterface;
import me.glux.acall.imp.rabbit.RabbitRequestReceiver;
import me.glux.acall.imp.rabbit.RabbitResultSender;
import me.glux.acall.server.AsynProxyServer;

public class SpringProxyServerExporter implements ApplicationContextAware {
	private static final Logger logger = LoggerFactory.getLogger(SpringProxyServerExporter.class);

	private String host;
	private String vhost;
	private String username;
	private String password;
	private String requestExchange;
	private String resultExchange;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<Class<?>, Object> toRegisterInterfaces = new HashMap<>();
		Map<String, Object> namedBeans = applicationContext.getBeansWithAnnotation(MbusService.class);

		for (Entry<String, Object> entry : namedBeans.entrySet()) {
			if (ProxyInterface.class.isInstance(entry.getValue())) {
				continue;
			}
			Class<?>[] interfaces = entry.getValue().getClass().getInterfaces();
			if (interfaces.length == 0)
				continue;
			for (Class<?> interfaze : interfaces) {
				if (!interfaze.isAnnotationPresent(MbusService.class)) {
					continue;
				}
				if (toRegisterInterfaces.containsKey(interfaze)) {
					Object handler = toRegisterInterfaces.get(interfaze);
					throw new IllegalStateException(interfaze.getName() + " has registerd for handler type "
							+ handler.getClass().getName() + ", and can not be register for "
							+ entry.getValue().getClass().getName() + " again.");
				}
				toRegisterInterfaces.put(interfaze, entry.getValue());
			}
		}
		createServer(toRegisterInterfaces);
	}

	private void createServer(Map<Class<?>, Object> interfaces) {
		if (interfaces == null || interfaces.size() == 0)
			return;
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setVirtualHost(vhost);
		factory.setUsername(username);
		factory.setPassword(password);
		Channel requestReceiveChannel;
		Channel resultSendChannel;
		try {
			requestReceiveChannel = factory.newConnection().createChannel();
			resultSendChannel = factory.newConnection().createChannel();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable create rabbit mq channel.", e);
		}
		String serverName;
		try {
			serverName = InetAddress.getLocalHost().getHostName() +'-'+ UUID.randomUUID().toString();
		} catch (UnknownHostException e) {
			logger.warn("Unable get host name.", e);
			serverName = UUID.randomUUID().toString();
		}
		RequestReceiver requestResceiver = new RabbitRequestReceiver(requestReceiveChannel, requestExchange);
		requestResceiver.registerServer(serverName);
		ResultSender resultSender = new RabbitResultSender(resultSendChannel, resultExchange);
		AsynProxyServer server = new AsynProxyServer(serverName, resultSender, requestResceiver);
		for (Entry<Class<?>, Object> entry : interfaces.entrySet()) {
			server.registerInterface(entry.getKey(), entry.getValue());
		}
		server.start();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRequestExchange() {
		return requestExchange;
	}

	public void setRequestExchange(String requestExchange) {
		this.requestExchange = requestExchange;
	}

	public String getResultExchange() {
		return resultExchange;
	}

	public void setResultExchange(String resultExchange) {
		this.resultExchange = resultExchange;
	}
}
