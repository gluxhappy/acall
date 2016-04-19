package me.glux.mbus;

import me.glux.mbus.protocal.RequestEntity;

public interface RequestSender {
	void send(RequestEntity request);
}
