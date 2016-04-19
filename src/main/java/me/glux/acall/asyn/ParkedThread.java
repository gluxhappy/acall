package me.glux.acall.asyn;

public class ParkedThread implements ParkedObject {
    private Object synctag = new Object();
    private Status status = Status.RUNNING;

    @Override
    public void timeout() {
        if (status != Status.RUNNING) {
            synchronized (synctag) {
                synctag.notify();
            }
        }
    }

    @Override
    public void park(long milliseconds) {
        status = Status.PARKING;
        synchronized (synctag) {
            long startTime = System.currentTimeMillis();
            boolean continueLoopFlag = true; // NOSONAR
            do {
                try {
                    synctag.wait(milliseconds);
                    continueLoopFlag = false;
                } catch (InterruptedException e) {
                    if (System.currentTimeMillis() - startTime < milliseconds) {
                        continueLoopFlag = true;
                    }
                }
            } while (continueLoopFlag);
        }
        status = Status.RUNNING;
    }

    @Override
    public void done() {
        if (status != Status.RUNNING) {
            synchronized (synctag) {
                synctag.notify();
            }
        }
    }

    private enum Status {
        RUNNING, PARKING;
    }
}
