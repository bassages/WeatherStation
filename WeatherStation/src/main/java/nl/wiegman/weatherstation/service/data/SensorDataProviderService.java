package nl.wiegman.weatherstation.service.data;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;

public interface SensorDataProviderService {

	String ACTION_AVAILABILITY_UPDATE = "nl.wiegman.weatherstation.service.data.AVAILABILITY";
	String AVAILABILITY_UPDATE_AVAILABLE = "nl.wiegman.weatherstation.service.data.AVAILABILITY_AVAILABLE";
	String AVAILABILITY_UPDATE_MESSAGEID = "nl.wiegman.weatherstation.service.data.AVAILABILITY_MESSAGEID";
	
	void addSensorValueListener(SensorValueListener sensorValueListener, SensorType ... sensorType);
	void removeSensorValueListener(SensorValueListener sensorValueListener, SensorType ... sensorType);
	
	/**
	 * Start publishing periodic sensor data updates to registered listeners
	 */
    void activate();
    
	/**
	 * Stop publishing periodic sensor data updates to registered listeners
	 */
    void deactivate();
}