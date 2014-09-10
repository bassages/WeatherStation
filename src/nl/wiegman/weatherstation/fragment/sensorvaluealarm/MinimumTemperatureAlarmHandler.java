package nl.wiegman.weatherstation.fragment.sensorvaluealarm;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.content.Context;

public class MinimumTemperatureAlarmHandler extends ValueAlarmHandler implements TemperatureValueChangeListener {

	public MinimumTemperatureAlarmHandler(Context context) {
		super(context);
	}
	
	@Override
	public void temperatureChanged(Context context, Double updatedValue) {
		super.valueChanged(context, updatedValue);
	}
	
	@Override
	protected boolean isAlarmConditionMet(Double updatedValue, Double alarmValue) {
		return updatedValue != null && updatedValue < alarmValue;
	}
	
	@Override
	protected String getValueAlarmEnabledPreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_minimum_temperature_enabled_key);
	}
	
	@Override
	protected String getValueAlarmValuePreferenceKey(Context context) {
		return context.getString(R.string.preference_alarm_minimum_temperature_value_key);
	}
	
	@Override
	protected String getAlarmNotificationText(Context context, Double updatedValue, Double alarmValue) {
		return context.getString(R.string.minimum_temperature_alarm_notification_text,
				TemperatureUtil.format(updatedValue),
				TemperatureUtil.getPreferredTemperatureUnit(context),
				TemperatureUtil.format(alarmValue),
				TemperatureUtil.getPreferredTemperatureUnit(context));
	}

	@Override
	protected String getValueAlarmNotificationTitle(Context context) {
		return context.getString(R.string.minimum_temperature_alarm_notification_title);
	}
}
