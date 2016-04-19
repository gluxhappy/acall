package me.glux.mbus;

import me.glux.mbus.protocal.ResponseEntity;

public interface ResultSender {
	void send(ResponseEntity response);
}
