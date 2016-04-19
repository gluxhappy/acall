package me.glux.mbus.export;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import me.glux.mbus.RequestSender;
import me.glux.mbus.ResultReceiver;
import me.glux.mbus.annotation.MbusService;
import me.glux.mbus.asyn.ParkedThread;
import me.glux.mbus.asyn.TimeoutObjectManager;
import me.glux.mbus.client.AsynProxyClient;
import me.glux.mbus.imp.rabbit.RabbitRequestSender;
import me.glux.mbus.imp.rabbit.RabbitResultReceiver;

public class SpringProxyClientExporter implements BeanFactoryPostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SpringProxyClientExporter.class);

	private List<Class<?>> intefaces;
	private String host;
	private String vhost;
	private String username;
	private String password;
	private String requestExchange;
	private String resultExchange;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (null == intefaces || intefaces.isEmpty()) {
			logger.warn("No interface proxyed.");
			return;
		}

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setVirtualHost(vhost);
		factory.setUsername(username);
		factory.setPassword(password);
		Channel requestSendChannel;
		Channel resultReceiveChannel;
		try {
			requestSendChannel = factory.newConnection().createChannel();
			resultReceiveChannel = factory.newConnection().createChannel();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable create rabbit mq channel.", e);
		}
		RequestSender requestSender = new RabbitRequestSender(requestSendChannel, requestExchange);

		String clientId = "client-";
		try {
			clientId += InetAddress.getLocalHost().getHostName() + '-' + UUID.randomUUID().toString();
		} catch (UnknownHostException e) {
			logger.warn("Unable get host name.", e);
			clientId += UUID.randomUUID().toString();
		}
		TimeoutObjectManager<ParkedThread> parkedTheadPool = new TimeoutObjectManager<>();
		ResultReceiver resultReceiver = new RabbitResultReceiver(resultReceiveChannel, clientId, resultExchange,
				parkedTheadPool);
		resultReceiver.setResultHoldTime(30 * 1000);
		AsynProxyClient client = new AsynProxyClient(clientId, requestSender, resultReceiver, parkedTheadPool);

		Object proxyObject = StaticExportor.createProxy(client, intefaces);
		beanFactory.registerSingleton(MbusService.class.getName() + '#' + System.currentTimeMillis(), proxyObject);
	}

	public void setProxyInterfaces(List<String> proxyInterfaces) {
		if (null == proxyInterfaces || proxyInterfaces.isEmpty())
			return;
		List<Class<?>> proxyInterfacesClass = new ArrayList<>(proxyInterfaces.size());
		for (String interfaceName : proxyInterfaces) {
			interfaceName=null==interfaceName?"":interfaceName.trim();
			if (interfaceName.isEmpty())
				continue;
			Class<?> interfaze;
			try {
				interfaze = Class.forName(interfaceName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Type [" + interfaceName + "] not found.", e);
			}
			if (!interfaze.isInterface()) {
				throw new IllegalArgumentException("Type [" + interfaceName + "] is not an interface.");
			}
			if (null == interfaze.getAnnotation(MbusService.class)) {
				throw new IllegalArgumentException("Interface [" + interfaceName + "] is not annotated with @"
						+ MbusService.class.getName() + ".");
			}
			proxyInterfacesClass.add(interfaze);
		}
		this.intefaces = proxyInterfacesClass;
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
