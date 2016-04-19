package me.glux.acall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.glux.acall.asyn.TimeoutObjectManager;
import me.glux.acall.protocal.ResponseEntity;

public abstract class ResultReceiver {
    private static final Logger logger = LoggerFactory.getLogger(ResultReceiver.class);
    private static final long DEFAULT_RESULT_HOLD_TIME = 5 * 1000;
    private ObjectMapper mapper = new ObjectMapper();
    private TimeoutObjectManager<ResponseEntity> responses = new TimeoutObjectManager<ResponseEntity>();
    private long resultHoldTime = DEFAULT_RESULT_HOLD_TIME;

    public long getResultHoldTime() {
        return resultHoldTime;
    }

    public void setResultHoldTime(long resultHoldTime) {
        this.resultHoldTime = resultHoldTime;
    }

    protected void start() {
        Thread receiverRunner = new Thread(new ReceiveRunner());
        receiverRunner.start();
    }

    public ResponseEntity get(String id) {
        return responses.get(id);
    }

    protected abstract byte[] receive();

    protected abstract void notifyRunner(String id);

    private class ReceiveRunner implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    byte[] resultBytes = receive();
                    if (null == resultBytes)
                        continue;
                    logger.debug("Received result:\n" + new String(resultBytes));
                    ResponseEntity response = mapper.readValue(resultBytes, ResponseEntity.class);
                    responses.put(response.getId(), resultHoldTime, response);
                    notifyRunner(response.getId());
                } catch (Exception e) {
                    logger.debug("Received invalid response.", e);
                }
            } while (true);
        }

    }
}
