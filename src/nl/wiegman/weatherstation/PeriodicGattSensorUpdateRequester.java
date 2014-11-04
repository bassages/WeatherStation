package nl.wiegman.weatherstation;

import java.util.List;

import nl.wiegman.weatherstation.gattsensor.GattSensor;

/**
 * Runnable that initiates a read on all registered gatt sensors
 */
public class PeriodicGattSensorUpdateRequester implements Runnable {

	private final List<GattSensor> gattSensors;

	public PeriodicGattSensorUpdateRequester(List<GattSensor> gattSensors) {
		this.gattSensors = gattSensors;
	}
	
	@Override
	public void run() {
		for (final GattSensor gattSensor: gattSensors) {
			gattSensor.read();	
		}
	}
}
