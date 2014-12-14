package nl.wiegman.weatherstation.service.data.impl.random;

import java.util.Random;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PeriodicRunnableExecutor;

import android.util.Log;

/**
 * Provides a random sensor value for test purposes
 */
public class RandomSensorDataValueService extends AbstractSensorDataProviderService {
	private static final String LOG_TAG = RandomSensorDataValueService.class.getSimpleName();

    private PeriodicRunnableExecutor periodicRunnableExecutor;

	@Override
	public void onDestroy() {
		super.onDestroy();
		deactivate();
	}
	
	@Override
	public void activate() {
		Log.d(LOG_TAG, "activate");

        SensorValueUpdateProducer runnable = new SensorValueUpdateProducer();
        periodicRunnableExecutor = new PeriodicRunnableExecutor("RandomSensorValueUpdateThread", runnable).start();
	}
    	
	@Override
	public void deactivate() {
		Log.d(LOG_TAG, "deactivate");
        periodicRunnableExecutor.stop();
	}
	
	private class SensorValueUpdateProducer implements Runnable {

		@Override
		public void run() {
			try {
				double randomAmbientTemperature = getRandom(0, 35);
				publishSensorValueUpdate(SensorType.AmbientTemperature, randomAmbientTemperature);

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
