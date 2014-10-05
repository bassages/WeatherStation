package nl.wiegman.weatherstation.history;

import java.util.List;

import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import android.content.Context;

/**
 * Maintains the temperature history
 */
public class TemperatureHistory implements TemperatureValueChangeListener {

	private static final String DB_SENSOR_NAME = "temperature";
	
	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
		SensorValueHistoryDatabase.getInstance(context).addSensorValue(DB_SENSOR_NAME, updatedTemperature);
	}

	public void deleteAll(Context context) {
		SensorValueHistoryDatabase.getInstance(context).deleteAll(DB_SENSOR_NAME);
	}
	
	public List<SensorValueHistoryItem> getAll(Context context) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory(DB_SENSOR_NAME);
	}
}
