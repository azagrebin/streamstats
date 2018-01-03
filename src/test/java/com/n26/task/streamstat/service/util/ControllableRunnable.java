package com.n26.task.streamstat.service.util;

import java.util.Random;

public class ControllableRunnable implements Runnable {
    private static final Random random = new Random();

    private final Runnable runnable;
    private final RunnableControl control;
    private final int runPeriod;

    public ControllableRunnable(Runnable runnable,
                                RunnableControl control,
                                int runPeriod) {
        this.runnable = runnable;
        this.control = control;
        this.runPeriod = runPeriod;
    }

    @Override
    public void run() {
        while (control.isRunning()) {
            try {
                control.checkAndPauseIfNeeded();
                Thread.sleep(random.nextInt(runPeriod));
            } catch (InterruptedException e) {
                return;
            }
            runnable.run();
        }
    }
}
