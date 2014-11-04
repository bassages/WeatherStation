package nl.wiegman.weatherstation;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Gives a sensible name to created threads
 */
public class GattSensorDataUpdateThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable runnable) {
		ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
		Thread newThread = defaultThreadFactory.newThread(runnable);
		newThread.setName("GattSensorUpdateRequesterThread");
		return newThread;
	}
}
