package nl.wiegman.weatherstation.preference;

import nl.wiegman.weatherstation.R;
import android.content.Context;
import android.util.AttributeSet;

public class MinimumTemperatureAlarmPreference extends TemperatureAlarmPreference {

    public MinimumTemperatureAlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
	protected int getAlarmEnabledPreferenceKey() {
		return R.string.preference_alarm_minimum_temperature_enabled_key;
	}
    
    @Override
	protected int getAlarmTemperaturePreferenceKey() {
		return R.string.preference_alarm_minimum_temperature_value_key;
	}
}