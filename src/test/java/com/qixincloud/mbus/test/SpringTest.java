package com.qixincloud.mbus.test;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import me.glux.mbus.client.ProxyInterface;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-config.xml" })
public class SpringTest extends AbstractJUnit4SpringContextTests {

	@Test
	public void testSpringDemo() {
		try {
			Demo proxyed = getProxyedDemo();
			String callResult = proxyed.call("test");
			logger.debug("Call Result:" + callResult);
		} catch (Exception e) {
			logger.warn("Error:", e);
			// throw new IllegalStateException("Error", e);
		}
	}

	private Demo getProxyedDemo() {
		Map<String, Demo> beans = this.applicationContext.getBeansOfType(Demo.class);
		for (Entry<String, Demo> entry : beans.entrySet()) {
			if (ProxyInterface.class.isInstance(entry.getValue())) {
				return entry.getValue();
			}
		}
		throw new IllegalStateException("Proxy " + Demo.class.getName() + " not found.");
	}
}
