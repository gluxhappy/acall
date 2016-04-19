package me.glux.acall.asyn;

public interface ParkedObject {
	void park(long time);
	void timeout();
	void done();
}
