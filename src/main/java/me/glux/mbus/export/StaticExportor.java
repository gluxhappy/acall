package me.glux.mbus.export;

import java.lang.reflect.Proxy;
import java.util.List;

import me.glux.mbus.client.AsynProxyClient;
import me.glux.mbus.client.ProxyInterface;

public final class StaticExportor {
	private StaticExportor() {
	}

	public static Object createProxy(AsynProxyClient client, List<Class<?>> interfaces) {
		interfaces.add(ProxyInterface.class);
		for (Class<?> interfaze : interfaces) {
			if (!interfaze.isInterface()) {
				throw new IllegalArgumentException(interfaze.getName() + " is not an interface.");
			}
		}
		Class<?>[] interfazes = interfaces.toArray(new Class<?>[] {});
		return Proxy.newProxyInstance(StaticExportor.class.getClassLoader(), interfazes, client);

	}

	public static <T> T createProxy(AsynProxyClient client, Class<T> clazz) {
		if (!clazz.isInterface()) {
			throw new IllegalArgumentException(clazz.getName() + " is not an interface.");
		}
		Class<?>[] interfazes = new Class<?>[] { ProxyInterface.class, clazz };
		return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), interfazes, client));
	}
}
