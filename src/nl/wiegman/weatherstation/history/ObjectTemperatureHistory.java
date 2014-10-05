package nl.wiegman.weatherstation.history;

import java.util.List;

import nl.wiegman.weatherstation.sensorvaluelistener.ObjectTemperatureListener;
import android.content.Context;

/**
 * Maintains the object temperature history
 */
public class ObjectTemperatureHistory implements ObjectTemperatureListener {

	private static final String DB_SENSOR_NAME = "object_temperature";
	
	@Override
	public void objectTemperatureUpdate(Context context, Double updatedTemperature) {
		SensorValueHistoryDatabase.getInstance(context).addSensorValue(DB_SENSOR_NAME, updatedTemperature);
	}

	public void deleteAll(Context context) {
		SensorValueHistoryDatabase.getInstance(context).deleteAll(DB_SENSOR_NAME);
	}
	
	public List<SensorValueHistoryItem> getAll(Context context) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory(DB_SENSOR_NAME);
	}
}
