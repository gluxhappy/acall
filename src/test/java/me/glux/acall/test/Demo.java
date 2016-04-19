package me.glux.acall.test;

import me.glux.acall.annotation.MbusApi;
import me.glux.acall.annotation.MbusService;

@MbusService("demo")
public interface Demo {
	@MbusApi("call")
	String call(String name);
}
