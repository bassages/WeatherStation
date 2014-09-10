package nl.wiegman.weatherstation.fragment.sensorvaluealarm;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import nl.wiegman.weatherstation.util.TemperatureUtil;
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

public class MaximumTemperatureAlarmHandler implements TemperatureValueChangeListener {

	private static final String LOG_TAG = MaximumTemperatureAlarmHandler.class.getSimpleName();
	
	private static final int MAXIMUM_TEMPERATURE_ALARM_NOTIFICATION_ID = 1;
	private static final String MAXIMUM_TEMPERATURE_ALARM_NOTIFICATION_TAG = MaximumTemperatureAlarmHandler.class.getSimpleName();
	
	private double DEFAULT_ALARM_VALUE = 0.0;
	
	private final String valueAlarmEnabledPreferenceKey;
	private final String valueAlarmValuePreferenceKey;
	
	private boolean alarmEnabled = false;
	private double alarmValue = DEFAULT_ALARM_VALUE;
	
	private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
	
	private NotificationCompat.Builder notificationBuilder;
	
	/**
	 * Constructor
	 */
	public MaximumTemperatureAlarmHandler(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		valueAlarmEnabledPreferenceKey = context.getString(R.string.preference_alarm_maximum_temperature_enabled_key);
		valueAlarmValuePreferenceKey = context.getString(R.string.preference_alarm_maximum_temperature_value_key);
		
		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		preferenceListener = new PreferenceListener();
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		// Set initial values
		preferenceListener.onSharedPreferenceChanged(sharedPreferences, valueAlarmEnabledPreferenceKey);
		preferenceListener.onSharedPreferenceChanged(sharedPreferences, valueAlarmValuePreferenceKey);
		
		logState();
	}
	
	@Override
	public void temperatureChanged(Context context, Double updatedValue) {
		if (updatedValue != null && alarmEnabled && updatedValue > alarmValue) {
			
			String message = String.format("%s %s > maximum of %s %s",
					TemperatureUtil.format(updatedValue),
					TemperatureUtil.getPreferredTemperatureUnit(context),
					TemperatureUtil.format(alarmValue),
					TemperatureUtil.getPreferredTemperatureUnit(context));
				
			if (notificationBuilder == null) {
				notificationBuilder = new NotificationCompat.Builder(context);
			}
			notificationBuilder
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Temperature alarm!")
				.setContentText(message)
				.setOnlyAlertOnce(true)
				.setAutoCancel(true)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			
			// Needed for autoCancel to work...
			PendingIntent notifyPIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);     
			notificationBuilder.setContentIntent(notifyPIntent);
			
			NotificationManager notificationyMgr = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
			notificationyMgr.notify(MAXIMUM_TEMPERATURE_ALARM_NOTIFICATION_TAG, MAXIMUM_TEMPERATURE_ALARM_NOTIFICATION_ID, notificationBuilder.build());
			
			Log.i(LOG_TAG, "Issue notification: " + message);
		}
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
		Log.i(LOG_TAG, "Temperature alarm enabled: " + alarmEnabled + " alarmValue: " + alarmValue);
	}
}
