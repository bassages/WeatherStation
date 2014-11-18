package nl.wiegman.weatherstation.service.alarm;

import nl.wiegman.weatherstation.service.alarm.impl.AlarmStrategy;

/**
 * Service which notifies the user when a sensor value gets higher/lower than a user customizable value
 */
public interface SensorValueAlarmService {

	/**
	 * Activates this service.
	 */
	void activate(Class<?> dataProviderServiceClass, AlarmStrategy ... strategies);

	/**
	 * De-activates this service.
	 */
	void deactivate();
}
