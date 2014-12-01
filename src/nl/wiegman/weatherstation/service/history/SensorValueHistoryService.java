package nl.wiegman.weatherstation.service.history;

import java.util.List;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.history.impl.SensorValueHistoryItem;
import android.content.Context;

/**
 * Service for storing/retrieving and cleanup of sensor values over time
 */
public interface SensorValueHistoryService {

	/**
	 * Activates this service. Begin collecting sensor updates.
	 */
	void activate();

	/**
	 * De-activates this service. Stop collecting sensor updates.
	 */
	void deactivate();

	/**
	 * @return a complete list of history of a given sensor type
	 */
	List<SensorValueHistoryItem> getAll(Context context, SensorType sensorType);

	/**
	 * Delete all history of all sensor types
	 */
	void deleteAll(Context context);
}
