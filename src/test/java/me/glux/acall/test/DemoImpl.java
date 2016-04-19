package me.glux.acall.test;

public class DemoImpl implements Demo {

	@Override
	public String call(String param) {
		return param + ", success.";
	}

}
