package nl.wiegman.weatherstation.history;

import java.util.List;

import nl.wiegman.weatherstation.sensorvaluelistener.AmbientTemperatureListener;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Maintains the ambient temperature history
 */
public class AmbientTemperatureHistory implements AmbientTemperatureListener {

	private static final String DB_SENSOR_NAME = "ambient_temperature";
	
	@Override
	public void ambientTemperatureUpdate(final Context context, final Double updatedTemperature) {
		// Do not block the UI thread, by using an aSyncTask
		AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SensorValueHistoryDatabase.getInstance(context).addSensorValue(DB_SENSOR_NAME, updatedTemperature);
				return null;
			}
		};
		asyncTask.execute();
	}

	public void deleteAll(final Context context) {
		AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SensorValueHistoryDatabase.getInstance(context).deleteAll(DB_SENSOR_NAME);
				return null;
			}
		};
		asyncTask.execute();
	}
	
	public List<SensorValueHistoryItem> getAll(Context context) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory(DB_SENSOR_NAME);
	}
}
