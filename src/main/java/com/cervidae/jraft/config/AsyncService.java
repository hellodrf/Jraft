package com.cervidae.jraft.config;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.*;

@Log4j2
@Component
public class AsyncService extends ThreadPoolExecutor implements AsyncUncaughtExceptionHandler {

    private static final int CPU_CORE_NUM = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_CORE_NUM + 1;
    private static final int MAX_POOL_SIZE = CPU_CORE_NUM * 2 + 1;
    private static final int QUEUE_CAPACITY = 128;
    private static final Long KEEP_ALIVE_TIME = 10L;

    @Autowired
    private AsyncService() {
        super(CORE_POOL_SIZE,
                MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY));
    }

    /**
     * Submit an async task to the thread pool
     * @param task runnable task
     * @return future
     */
    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(task);
    }

    /**
     * Submit an async task to the thread pool
     * @param task runnable task
     */
    public void go(Runnable task) {
        submit(task);
    }

    public boolean close() {
        super.shutdown();
        try {
            return super.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean closeNow() {
        super.shutdownNow();
        try {
            return super.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SneakyThrows
    @Override
    public void handleUncaughtException(Throwable throwable, @NonNull Method method, @NonNull Object... objects) {
        log.error(Throwable.class.getName() + " was thrown in @Async function [" + method.getName() +
                "], dumping stacktrace:");
        throwable.printStackTrace();
    }
}
