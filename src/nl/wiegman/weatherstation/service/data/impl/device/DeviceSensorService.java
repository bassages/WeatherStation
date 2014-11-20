package nl.wiegman.weatherstation.service.data.impl.device;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.util.NamedThreadFactory;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class DeviceSensorService extends AbstractSensorDataProviderService implements SensorEventListener {
	private final String LOG_TAG = this.getClass().getSimpleName();

	private static final long PUBLISH_RATE_IN_MILLISECONDS = 10000;
		
	private ScheduledExecutorService periodicSensorValueUpdateProducerExecutor;

	private SensorManager sensorManager;
	
	private double ambientTemperature;
	private double objectTemperature;
	private double humidity;
	private double airPressure;
	
	@Override
	public void activate() {
		// Get an instance of the sensor service, and use that to get an instance of
	    // a particular sensor.
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		Sensor ambientTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		sensorManager.registerListener(this, ambientTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);

		Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);

		Sensor humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		if (periodicSensorValueUpdateProducerExecutor == null) {
			periodicSensorValueUpdateProducerExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("DeviceSensorValueUpdateThread"));
			int startDelay = 500;
			SensorValueProducer periodicGattSensorUpdateRequester = new SensorValueProducer();
			periodicSensorValueUpdateProducerExecutor.scheduleWithFixedDelay(periodicGattSensorUpdateRequester, startDelay, PUBLISH_RATE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		}
	}
    	
	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(this);
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
	
	private class SensorValueProducer implements Runnable {

		@Override
		public void run() {
			try {
				Log.d(LOG_TAG, "Update temperature to: " + ambientTemperature);
				Log.d(LOG_TAG, "Update humidity to: " + humidity);
				Log.d(LOG_TAG, "Update air pressure to: " + airPressure);
	            
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.AmbientTemperature)) {
					listener.valueUpdate(getApplicationContext(), SensorType.AmbientTemperature, ambientTemperature);
	            }
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.ObjectTemperature)) {
					listener.valueUpdate(getApplicationContext(), SensorType.ObjectTemperature, objectTemperature);
	            }
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.Humidity)) {
					listener.valueUpdate(getApplicationContext(), SensorType.Humidity, humidity);
	            }
				for (SensorValueListener listener : sensorValueListeners.get(SensorType.AirPressure)) {
					listener.valueUpdate(getApplicationContext(), SensorType.AirPressure, airPressure);
	            }
			} catch (Exception e) {
				Log.wtf(LOG_TAG, e);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Ignore
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			ambientTemperature = event.values[0];	
		} else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			airPressure = event.values[0];
		} else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			humidity = event.values[0];	
		}
	}

}
