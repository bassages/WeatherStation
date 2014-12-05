package nl.wiegman.weatherstation.service.alarm.impl;

import static nl.wiegman.weatherstation.util.TemperatureUtil.*;
import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import android.content.Context;

public class MinimumTemperatureAlarm implements AlarmStrategy {

	@Override
	public SensorType getSensorType() {
		return SensorType.AmbientTemperature;
	}

	@Override
	public String getValueAlarmEnabledPreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_minimum_temperature_enabled_key);
	}

	@Override
	public String getValueAlarmValuePreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_minimum_temperature_value_key);
	}

	@Override
	public boolean isAlarmConditionMet(Double updatedValue, Double alarmValue) {
		return updatedValue != null && round(updatedValue) < round(alarmValue);
	}

	@Override
	public String getValueAlarmNotificationTitle(Context context) {
		return context.getString(R.string.minimum_temperature_alarm_notification_title);
	}

	@Override
	public String getAlarmNotificationText(Context context, Double updatedValue, Double alarmValue) {
		return context.getString(R.string.minimum_temperature_alarm_notification_text,
				format(convertFromStorageUnitToPreferenceUnit(context, updatedValue)),
				getPreferredTemperatureUnit(context),
				format(convertFromStorageUnitToPreferenceUnit(context, alarmValue)),
				getPreferredTemperatureUnit(context));
	}
}
