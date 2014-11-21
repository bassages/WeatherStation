package nl.wiegman.weatherstation.service.data.impl.random;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.util.NamedThreadFactory;
import android.util.Log;

/**
 * Supplies a random sensor value for test purposes
 */
public class RandomSensorDataValueService extends AbstractSensorDataProviderService {
	private static final String LOG_TAG = RandomSensorDataValueService.class.getSimpleName();

	private static final long PUBLISH_RATE_IN_MILLISECONDS = 3000;
		
	private ScheduledExecutorService periodicSensorValueUpdateProducerExecutor;

	@Override
	public void activate() {
		if (periodicSensorValueUpdateProducerExecutor == null) {
			periodicSensorValueUpdateProducerExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("RandomSensorValueUpdateThread"));
			int startDelay = 500;
			SensorValueUpdateProducer periodicGattSensorUpdateRequester = new SensorValueUpdateProducer();
			periodicSensorValueUpdateProducerExecutor.scheduleWithFixedDelay(periodicGattSensorUpdateRequester, startDelay, PUBLISH_RATE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		}
	}
    	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopSensorDataUpdates();
	}
	
	private void stopSensorDataUpdates() {
        if (periodicSensorValueUpdateProducerExecutor != null) {
        	periodicSensorValueUpdateProducerExecutor.shutdown();
            try {
            	periodicSensorValueUpdateProducerExecutor.awaitTermination(5, TimeUnit.SECONDS);
            	periodicSensorValueUpdateProducerExecutor = null;
    		} catch (InterruptedException e) {
    			Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
    		}
        }
	}

	private class SensorValueUpdateProducer implements Runnable {

		@Override
		public void run() {
			try {
				double randomAmbientTemperature = getRandom(0, 35);
				publishSensorValueUpdate(SensorType.AmbientTemperature, randomAmbientTemperature);

				double randomObjectTemperature = getRandom(0, 100);
				publishSensorValueUpdate(SensorType.ObjectTemperature, randomObjectTemperature);

				double randomHumidity = getRandom(0, 100);
				publishSensorValueUpdate(SensorType.Humidity, randomHumidity);

				double randomAirPressure = getRandom(950, 1050);
				publishSensorValueUpdate(SensorType.AirPressure, randomAirPressure);
			} catch (Exception e) { // Catch exceptions to make sure the executorService keeps running
				Log.wtf(LOG_TAG, e);
			}
		}
	}
	
	private double getRandom(int low, int high) {
		Random r = new Random();
		int result = r.nextInt(high-low) + low;
		
		return (double) result;
	}
}
