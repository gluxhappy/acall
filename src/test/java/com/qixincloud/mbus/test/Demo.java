package com.qixincloud.mbus.test;

import me.glux.mbus.annotation.MbusApi;
import me.glux.mbus.annotation.MbusService;

@MbusService("demo")
public interface Demo {
	@MbusApi("call")
	String call(String name);
}
