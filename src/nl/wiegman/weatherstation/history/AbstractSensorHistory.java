package nl.wiegman.weatherstation.history;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

public abstract class AbstractSensorHistory {

	public void registerUpdatedValue(final Context context, final Double updatedSensorValue) {
		// Do not block the UI thread, by using an aSyncTask
		AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SensorValueHistoryDatabase.getInstance(context).addSensorValue(getSensorName(), updatedSensorValue);
				return null;
			}
		};
		asyncTask.execute();
	}

	public void deleteAll(final Context context) {
		AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SensorValueHistoryDatabase.getInstance(context).deleteAll(getSensorName());
				return null;
			}
		};
		asyncTask.execute();
	}
	
	public List<SensorValueHistoryItem> getAll(Context context) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory(getSensorName());
	}
	
	protected abstract String getSensorName();
}
