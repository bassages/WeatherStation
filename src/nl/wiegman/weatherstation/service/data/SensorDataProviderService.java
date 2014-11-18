package nl.wiegman.weatherstation.service.data;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;

public interface SensorDataProviderService {
	
	void addSensorValueListener(SensorValueListener sensorValueListener, SensorType sensorType);
	void removeSensorValueListener(SensorValueListener sensorValueListener, SensorType sensorType);
	
	/**
	 * Start publishing periodic sensor data updates to registered listeners
	 */
    void activate();
}