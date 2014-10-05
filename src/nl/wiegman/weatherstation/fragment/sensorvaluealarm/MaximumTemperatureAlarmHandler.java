package nl.wiegman.weatherstation.fragment.sensorvaluealarm;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.sensorvaluelistener.AmbientTemperatureListener;
import static nl.wiegman.weatherstation.util.TemperatureUtil.*;
import android.content.Context;

/**
 * Issues a notification when the temperature exceeds the user defined value
 */
public class MaximumTemperatureAlarmHandler extends ValueAlarmHandler implements AmbientTemperatureListener {

	public MaximumTemperatureAlarmHandler(Context context) {
		super(context);
	}
	
	@Override
	public void ambientTemperatureUpdate(Context context, Double updatedValue) {
		super.valueChanged(context, updatedValue);
	}
	
	@Override
	protected boolean isAlarmConditionMet(Double updatedValue, Double alarmValue) {
		return updatedValue != null && round(updatedValue) > round(alarmValue);
	}
	
	@Override
	protected String getValueAlarmEnabledPreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_maximum_temperature_enabled_key);
	}
	
	@Override
	protected String getValueAlarmValuePreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_maximum_temperature_value_key);
	}
	
	@Override
	protected String getAlarmNotificationText(Context context, Double updatedValue, Double alarmValue) {
		return context.getString(R.string.maximum_temperature_alarm_notification_text,
				format(updatedValue),
				getPreferredTemperatureUnit(context),
				format(alarmValue),
				getPreferredTemperatureUnit(context));
	}

	@Override
	protected String getValueAlarmNotificationTitle(Context context) {
		return context.getString(R.string.maximum_temperature_alarm_notification_title);
	}
}
