package com.n26.task.streamstat.service.util;

import java.util.concurrent.CountDownLatch;

public class RunnableControl {
    private final int workersNum;
    private volatile boolean stopped;
    private volatile boolean paused;
    private volatile CountDownLatch pausedRunnables;
    private volatile CountDownLatch resumed;

    public RunnableControl(int workersNum) {
        this.workersNum = workersNum;
    }

    public void pause() throws InterruptedException {
        if (!paused && !stopped) {
            pausedRunnables = new CountDownLatch(workersNum);
            resumed = new CountDownLatch(1);
            paused = true;
            pausedRunnables.await();
        }
    }

    void checkAndPauseIfNeeded() throws InterruptedException {
        while (paused && !stopped) {
            pausedRunnables.countDown();
            resumed.await();
        }
    }

    public void resume() {
        if (!stopped) {
            paused = false;
            resumed.countDown();
        }
    }

    public void stop() {
        stopped = true;
    }

    boolean isRunning() {
        return !stopped;
    }
}
