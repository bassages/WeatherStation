package nl.wiegman.weatherstation.history;

import java.util.List;

import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import android.content.Context;
import android.util.Log;

public class TemperatureHistory implements TemperatureValueChangeListener {

	private final String LOG_TAG = this.getClass().getSimpleName();
	
	private static final String DB_SENSOR_NAME_TEMPERATURE = "temperature";
	
	private HistoryDatabase db = null;
	
	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
		HistoryDatabase database = getDatabase(context);
		database.addSensorValue(DB_SENSOR_NAME_TEMPERATURE, updatedTemperature);

		logAllHistory(database);
	}

	private void logAllHistory(HistoryDatabase database) {
		List<SensorHistoryItem> historyItems = database.getAllHistory();
		Log.i(LOG_TAG, "----------- History ------------");
        for (SensorHistoryItem historyItem : historyItems) {
            String log = "History item: Id="+ historyItem.getId()+", TimeStamp=" + historyItem.getTimestamp() + ", Sensor name=" + historyItem.getSensorName() + ", Sensor value=" + historyItem.getSensorValue();
            Log.i(LOG_TAG, log);
        }
	}

	public void deleteAll(Context context) {
		getDatabase(context).deleteAll(DB_SENSOR_NAME_TEMPERATURE);
	}
	
	private HistoryDatabase getDatabase(Context context) {
		if (db == null) {
			db = new HistoryDatabase(context);			
		}
		return db;
	}
}
