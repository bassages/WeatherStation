package nl.wiegman.weatherstation.fragment.sensorvaluealarm;

import nl.wiegman.weatherstation.R;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public abstract class ValueAlarmHandler {

	private final String LOG_TAG = this.getClass().getSimpleName();
	
	private static final int ALARM_NOTIFICATION_ID = 1;
	private final String ALARM_NOTIFICATION_TAG = this.getClass().getSimpleName();
	
	private double DEFAULT_ALARM_VALUE = 0.0;
	
	private final String valueAlarmEnabledPreferenceKey;
	private final String valueAlarmValuePreferenceKey;
	
	private boolean alarmEnabled = false;
	private double alarmValue = DEFAULT_ALARM_VALUE;
	
	private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
	
	private NotificationCompat.Builder notificationBuilder;
	
	public ValueAlarmHandler(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		valueAlarmEnabledPreferenceKey = getValueAlarmEnabledPreferenceKey(context);
		valueAlarmValuePreferenceKey = getValueAlarmValuePreferenceKey(context);
		
		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		preferenceListener = new PreferenceListener();
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		// Set initial values
		preferenceListener.onSharedPreferenceChanged(sharedPreferences, valueAlarmEnabledPreferenceKey);
		preferenceListener.onSharedPreferenceChanged(sharedPreferences, valueAlarmValuePreferenceKey);
		
		logState();
	}

	protected abstract String getValueAlarmValuePreferenceKey(Context context);

	protected abstract String getValueAlarmEnabledPreferenceKey(Context context);
	
	protected abstract String getAlarmNotificationText(Context context, Double updatedValue, Double alarmValue);
	
	protected abstract String getValueAlarmNotificationTitle(Context context);
	
	protected abstract boolean isAlarmConditionMet(Double updatedValue, Double alarmValue);
	
	protected void valueChanged(Context context, Double updatedValue) {
		if (alarm(updatedValue)) {
			createNotification(context, updatedValue);
		}
	}

	private void createNotification(Context context, Double updatedValue) {
		String message = getAlarmNotificationText(context, updatedValue, alarmValue);
		
		Log.i(this.getClass().getSimpleName(), "Notification message: " + message);
		
		configureNotificationBuilder(context, message);
		
		// Needed for autoCancel to work...
		PendingIntent notifyPIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);     
		notificationBuilder.setContentIntent(notifyPIntent);
		
		NotificationManager notificationyMgr = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
		notificationyMgr.notify(ALARM_NOTIFICATION_TAG, ALARM_NOTIFICATION_ID, notificationBuilder.build());
	}

	private void configureNotificationBuilder(Context context, String message) {
		if (notificationBuilder == null) {
			notificationBuilder = new NotificationCompat.Builder(context);
		}
		notificationBuilder
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(getValueAlarmNotificationTitle(context))
			.setContentText(message)
			.setOnlyAlertOnce(true)
			.setAutoCancel(true)
			.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
	}

	private boolean alarm(Double updatedValue) {
		return alarmEnabled && isAlarmConditionMet(updatedValue, alarmValue);
	}
		
	/**
	 * Handles changes in the alarm preferences by updating the fields
	 */
	private class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(valueAlarmEnabledPreferenceKey)) {
				alarmEnabled = sharedPreferences.getBoolean(key, false);
				if (!alarmEnabled) {
					alarmValue = DEFAULT_ALARM_VALUE;
				}
			} else if (key.equals(valueAlarmValuePreferenceKey)) {
				alarmValue = (double) sharedPreferences.getFloat(key, 0.0f);
			}
			logState();
		}
	}
	
	private void logState() {
		Log.i(LOG_TAG, "Alarm enabled: " + alarmEnabled + " alarmValue: " + alarmValue);
	}
}
