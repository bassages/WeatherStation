package nl.wiegman.weatherstation.service.data.impl.device;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PeriodicRunnableExecutor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Provides data from the internal sensors of the device
 */
public class DeviceSensorService extends AbstractSensorDataProviderService implements SensorEventListener {
	private final String LOG_TAG = this.getClass().getSimpleName();

	private PeriodicRunnableExecutor periodicSensorValueUpdateProducer;

	private SensorManager sensorManager;
	
	private Double ambientTemperature;
	private Double humidity;
	private Double airPressure;
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		deactivate();
	}
	
	@Override
	public void activate() {
		Log.d(LOG_TAG, "activate");
		
		// Get an instance of the sensor service, and use that to get an instance of a particular sensor.
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		registerAsAmbientTemperatureListener();
		registerAsAirPressureListener();
		registerAsHumidityListener();
		
		startProvidingSensorValuesToListeners();
	}

	@Override
	public void deactivate() {
		Log.d(LOG_TAG, "deactivate");
		
		sensorManager.unregisterListener(this);
		stopSensorDataUpdates();
		sensorManager = null;

        ambientTemperature = null;
        humidity = null;
        airPressure = null;

		super.deactivate();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Ignore
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			ambientTemperature = (double)event.values[0];	
		} else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			airPressure = (double)event.values[0];
		} else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			humidity = (double)event.values[0];
		}
	}
	
	private void registerAsAmbientTemperatureListener() {
		Sensor ambientTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		if (ambientTemperatureSensor == null) {
			Log.w(LOG_TAG, "There is no ambient temperature sensor available on this device");
		} else {
			sensorManager.registerListener(this, ambientTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	private void registerAsHumidityListener() {
		Sensor humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		if (humiditySensor == null) {
			Log.w(LOG_TAG, "There is no humidity sensor available on this device");
		} else {
			sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	private void registerAsAirPressureListener() {
		Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		if (pressureSensor == null) {
			Log.w(LOG_TAG, "There is no air pressure sensor available on this device");
		} else {
			sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	private void startProvidingSensorValuesToListeners() {
		periodicSensorValueUpdateProducer = new PeriodicRunnableExecutor("DeviceSensorValueUpdateThread", new SensorValueProducer()).start();
	}

	private void stopSensorDataUpdates() {
        periodicSensorValueUpdateProducer.stop();
	}
	
	private class SensorValueProducer implements Runnable {
		@Override
		public void run() {
			try {
				publishSensorValueUpdate(SensorType.AmbientTemperature, ambientTemperature);
				publishSensorValueUpdate(SensorType.Humidity, humidity);
				publishSensorValueUpdate(SensorType.AirPressure, airPressure);
			} catch (Exception e) { // Catch exceptions to make sure the executorService keeps running
				Log.wtf(LOG_TAG, e);
			}
		}
	}
}
