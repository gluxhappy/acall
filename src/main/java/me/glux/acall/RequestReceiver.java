package me.glux.acall;

import me.glux.acall.protocal.RequestEntity;

/**
 * RequestReceiver for receiving acall request from remote(local queue or mq server)
 */
public interface RequestReceiver {
	/**
	 * Register current server name as 'server'
	 * @param server name of server
     */
	void registerServer(String server);
	void registerApi(String api);
	RequestEntity receive() throws InterruptedException;
}
