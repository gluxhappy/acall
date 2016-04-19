package me.glux.mbus.asyn;

public interface ParkedObject {
	void park(long time);
	void timeout();
	void done();
}
