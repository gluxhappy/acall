package me.glux.mbus.asyn;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutObjectManager<T> {
    private Map<String, WarpedObject> cache = new ConcurrentHashMap<>(1024);

    public TimeoutObjectManager() {
        init();
    }

    void init() {
        Thread gcThread = new Thread(new GcTask());
        gcThread.start();
    }

    public void put(String id, long timeout, T object) {
        cache.put(id, new WarpedObject(timeout, object));
    }

    public T get(String id) {
        T rt = null;
        WarpedObject result;
        result = cache.get(id);
        if (null != result) {
            cache.remove(id);
            rt = result.get();
        }
        return rt;
    }

    private class WarpedObject {
        public final long createTime = System.currentTimeMillis();
        private final long timeout;
        private T object;

        public WarpedObject(long timeout, T object) {
            this.timeout = timeout;
            this.object = object;
        }

        public boolean isTimeout(long current) {
            return current - createTime >= timeout;
        }

        public T get() {
            return object;
        }
    }

    private class GcTask implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(GcTask.class);
        private static final long DEFAULT_PERIDO = 30 * 1000L;
        private long perido = DEFAULT_PERIDO;

        @Override
        public void run() {
            // Starting gc loop
            while (true) {
                try {
                    Thread.sleep(perido);
                } catch (InterruptedException e) {
                    logger.warn("Gc thread sleep interupted", e);
                }
                doGC();
            }
        }
        
        private void doGC(){
            long current = System.currentTimeMillis();
            Set<String> timeoutObjects = new TreeSet<>();

            // 提取所有超时的对象
            for (Entry<String, WarpedObject> entry : cache.entrySet()) {
                WarpedObject value = entry.getValue();
                try {
                    if (value.isTimeout(current)) {
                        timeoutObjects.add(entry.getKey());
                    }
                } catch (Exception e) {
                    logger.warn("Run thread gc error", e);
                }
            }
            for (String key : timeoutObjects) {
                cache.remove(key);
            }
        }
    }
}
