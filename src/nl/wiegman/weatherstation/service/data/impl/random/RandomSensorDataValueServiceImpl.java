package nl.wiegman.weatherstation.service.data.impl.random;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.util.NamedThreadFactory;
import android.content.Intent;
import android.util.Log;

/**
 * Supplies a random sensor value for test purposes
 */
public class RandomSensorDataValueServiceImpl extends AbstractSensorDataProviderService {

	private static final String LOG_TAG = RandomSensorDataValueServiceImpl.class.getSimpleName();

	private static final long SENSORS_REFRESH_RATE_IN_MILLISECONDS = 3000;
		
	private ScheduledExecutorService periodicSensorUpdateExecutor;

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

	@Override
	public void activate() {
		if (periodicSensorUpdateExecutor == null) {
			periodicSensorUpdateExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("RandomSensorValueUpdateThread"));
			int startDelay = 500;
			SensorValueProducer periodicGattSensorUpdateRequester = new SensorValueProducer();
			periodicSensorUpdateExecutor.scheduleWithFixedDelay(periodicGattSensorUpdateRequester, startDelay, SENSORS_REFRESH_RATE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		}
	}
    	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopSensorDataUpdates();
	}
	
	private void stopSensorDataUpdates() {
        if (periodicSensorUpdateExecutor != null) {
        	periodicSensorUpdateExecutor.shutdown();
            try {
            	periodicSensorUpdateExecutor.awaitTermination(5, TimeUnit.SECONDS);
            	periodicSensorUpdateExecutor = null;
    		} catch (InterruptedException e) {
    			Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
    		}
        }
	}

	private class SensorValueProducer implements Runnable {

		@Override
		public void run() {
			try {
				double randomTemperature = getRandom(10, 20);
				double randomHumidity = getRandom(0, 100);
				double randomAirPressure = getRandom(950, 1050);

				Log.d(LOG_TAG, "Update temperature to: " + randomTemperature);
				Log.d(LOG_TAG, "Update humidity to: " + randomHumidity);
				Log.d(LOG_TAG, "Update air pressure to: " + randomAirPressure);
	            
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.AmbientTemperature)) {
					listener.valueUpdate(getApplicationContext(), SensorType.AmbientTemperature, randomTemperature);
	            }
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.Humidity)) {
					listener.valueUpdate(getApplicationContext(), SensorType.Humidity, randomHumidity);
	            }
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.AirPressure)) {
					listener.valueUpdate(getApplicationContext(), SensorType.AirPressure, randomAirPressure);
	            }
			} catch (Exception e) {
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
