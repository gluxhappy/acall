package me.glux.acall;

import me.glux.acall.protocal.ResponseEntity;

public interface ResultSender {
	void send(ResponseEntity response);
}
