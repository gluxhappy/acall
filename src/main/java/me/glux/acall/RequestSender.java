package me.glux.acall;

import me.glux.acall.protocal.RequestEntity;

public interface RequestSender {
	void send(RequestEntity request);
}
