package me.glux.mbus;

import me.glux.mbus.protocal.RequestEntity;

public interface RequestReceiver {
	void registerServer(String server);
	void registerApi(String api);
	RequestEntity receive() throws InterruptedException;
}
