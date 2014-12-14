package nl.wiegman.weatherstation.service.data.impl;

import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.util.NamedThreadFactory;

public class PeriodicRunnableExecutor {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final long PUBLISH_RATE_IN_MILLISECONDS = 10000;

    private long publishRate = PUBLISH_RATE_IN_MILLISECONDS;

    private ScheduledExecutorService executorService;

    private final String threadName;
    private final Runnable runnable;

    public PeriodicRunnableExecutor(String threadName, Runnable runnable) {
        this.threadName = threadName;
        this.runnable = runnable;
    }

    public PeriodicRunnableExecutor setPublishRate(long publishRateInMilliseconds) {
        this.publishRate = publishRateInMilliseconds;
        return this;
    }

    public PeriodicRunnableExecutor start() {
        if (executorService == null) {
            executorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(threadName));
            int startDelay = 500;
            executorService.scheduleWithFixedDelay(runnable, startDelay, publishRate, TimeUnit.MILLISECONDS);
        }
        return this;
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
                executorService = null;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
            }
        }
    }
}
