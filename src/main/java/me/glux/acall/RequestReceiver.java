package me.glux.acall;

import me.glux.acall.protocal.RequestEntity;

public interface RequestReceiver {
	void registerServer(String server);
	void registerApi(String api);
	RequestEntity receive() throws InterruptedException;
}
