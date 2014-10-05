package nl.wiegman.weatherstation.history;

import java.util.List;

import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import android.content.Context;

public class TemperatureHistoryStore implements TemperatureValueChangeListener {

	private static final String DB_SENSOR_NAME_TEMPERATURE = "temperature";
	
	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
		SensorValueHistoryDatabase.getInstance(context).addSensorValue(DB_SENSOR_NAME_TEMPERATURE, updatedTemperature);
	}

	public void deleteAll(Context context) {
		SensorValueHistoryDatabase.getInstance(context).deleteAll(DB_SENSOR_NAME_TEMPERATURE);
	}
	
	public List<SensorHistoryItem> getAll(Context context) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory();
	}
}
