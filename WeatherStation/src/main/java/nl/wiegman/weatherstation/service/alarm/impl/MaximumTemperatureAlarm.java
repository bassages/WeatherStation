package nl.wiegman.weatherstation.service.alarm.impl;

import static nl.wiegman.weatherstation.util.TemperatureUtil.convertFromStorageUnitToPreferenceUnit;
import static nl.wiegman.weatherstation.util.TemperatureUtil.format;
import static nl.wiegman.weatherstation.util.TemperatureUtil.getPreferredTemperatureUnit;
import static nl.wiegman.weatherstation.util.TemperatureUtil.round;
import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import android.content.Context;

/**
 * Issues a notification when the temperature exceeds the user defined value
 */
public class MaximumTemperatureAlarm extends SensorValueAlarmServiceImpl implements AlarmStrategy {

	@Override
	public SensorType getSensorType() {
		return SensorType.AmbientTemperature;
	}
		
	@Override
	public String getValueAlarmEnabledPreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_maximum_temperature_enabled_key);
	}
	
	@Override
	public String getValueAlarmValuePreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_maximum_temperature_value_key);
	}

	@Override
	public boolean isAlarmConditionMet(Double updatedValue, Double alarmValue) {
		return updatedValue != null && round(updatedValue) > round(alarmValue);
	}

	@Override
	public String getValueAlarmNotificationTitle(Context context) {
		return context.getString(R.string.maximum_temperature_alarm_notification_title);
	}
	
	@Override
	public String getAlarmNotificationText(Context context, Double updatedValueInStorageUnit, Double alarmValueInStorageUnit) {
		return context.getString(R.string.maximum_temperature_alarm_notification_text,
				format(convertFromStorageUnitToPreferenceUnit(context, updatedValueInStorageUnit)),
				getPreferredTemperatureUnit(context),
				format(convertFromStorageUnitToPreferenceUnit(context, alarmValueInStorageUnit)),
				getPreferredTemperatureUnit(context));
	}
}
