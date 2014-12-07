package nl.wiegman.weatherstation.preference;

import nl.wiegman.weatherstation.R;
import android.content.Context;
import android.util.AttributeSet;

/**
 * Maximum temperature alarm preference
 */
public class MaximumTemperatureAlarmPreference extends TemperatureAlarmPreference {

    public MaximumTemperatureAlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
	protected int getAlarmEnabledPreferenceKey() {
		return R.string.preference_alarm_maximum_temperature_enabled_key;
	}
    
    @Override
	protected int getAlarmTemperaturePreferenceKey() {
		return R.string.preference_alarm_maximum_temperature_value_key;
	}
}