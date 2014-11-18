package nl.wiegman.weatherstation.service.alarm.impl;

import nl.wiegman.weatherstation.SensorType;
import android.content.Context;

public interface AlarmStrategy {

	SensorType getSensorType();
	
	boolean isAlarmConditionMet(Double updatedValue, Double alarmValue);
	
	String getAlarmNotificationText(Context context, Double updatedValueInStorageUnit, Double alarmValueInStorageUnit);	
	String getValueAlarmNotificationTitle(Context context);
	
	String getValueAlarmEnabledPreferenceKey(Context context);
	String getValueAlarmValuePreferenceKey(Context context);

}
