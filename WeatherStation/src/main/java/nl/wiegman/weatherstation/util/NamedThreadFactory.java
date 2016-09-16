package nl.wiegman.weatherstation.util;

import android.support.annotation.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Gives a sensible name to created threads
 */
public class NamedThreadFactory implements ThreadFactory {

	private final String threadName;
	
	public NamedThreadFactory(String threadName) {
		this.threadName = threadName;
	}
	
	@Override
	public Thread newThread(@NonNull Runnable runnable) {
		ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
		Thread newThread = defaultThreadFactory.newThread(runnable);
		newThread.setName(threadName);
		return newThread;
	}
}
