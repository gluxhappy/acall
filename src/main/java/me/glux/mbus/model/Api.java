package me.glux.mbus.model;

import java.lang.reflect.Method;

public class Api {
	private Method method;
	private Object handler;

	public Api(Method method, Object handler) {
		this.method = method;
		this.handler = handler;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getHandler() {
		return handler;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}
}