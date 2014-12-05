package nl.wiegman.weatherstation.preference;

import org.apache.commons.lang3.StringUtils;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

public abstract class TemperatureAlarmPreference extends DialogPreference implements OnClickListener {

    private final String LOG_TAG = this.getClass().getSimpleName();
    
    private CheckBox alarmEnabledCheckBox;
    private TextView alarmValueTextView;
    private TextView alarmValueUnitLabelTextView;

    public TemperatureAlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_alarm);
    }
    
	protected abstract int getAlarmEnabledPreferenceKey();

	protected abstract int getAlarmTemperaturePreferenceKey();
    
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		alarmEnabledCheckBox = (CheckBox) view.findViewById(R.id.preference_alarm_enabled_checkbox);
		alarmValueTextView = (TextView) view.findViewById(R.id.preference_alarm_temperature_value_textview);
		alarmValueUnitLabelTextView = (TextView) view.findViewById(R.id.preference_alarm_temperature_unit_label);
		
		setAlarmValueUnitLabelBasedOnPreference();
		
		boolean alarmEnabledPreferenceValue = setAlarmEnabledCheckBoxValueBasedOnPreference();
		if (alarmEnabledPreferenceValue) {
			setAlarmValueBasedOnPreference();
		}
		
		alarmEnabledCheckboxChanged();
		alarmEnabledCheckBox.setOnClickListener(this);
	}

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.i(LOG_TAG, "onDialogClosed(positiveResult=" + positiveResult + ")");
        
        if (positiveResult) {
            Editor editor = getEditor();
            
            String alarmEnabledPreferenceKey = getContext().getString(getAlarmEnabledPreferenceKey());
            String alarmValuePreferenceKey = getContext().getString(getAlarmTemperaturePreferenceKey());
            
            boolean alarmEnabled = alarmEnabledCheckBox.isChecked();
            Log.i(LOG_TAG, "Setting preference " + alarmEnabledPreferenceKey + " to " + alarmEnabled);
            editor.putBoolean(alarmEnabledPreferenceKey, alarmEnabled);

            CharSequence alarmValueAsString = alarmValueTextView.getText();
            if (alarmEnabled && StringUtils.isNotBlank(alarmValueAsString)) {
               	float minimumTemperatureValueAsFloat = Float.parseFloat(alarmValueAsString.toString());
               	
               	double alarmValueInStorageUnit = TemperatureUtil.convertFromPreferenceUnitToStorageUnit(getContext(), minimumTemperatureValueAsFloat);
               	
               	Log.i(LOG_TAG, "Setting preference " + alarmValuePreferenceKey + " to " + alarmValueInStorageUnit);
               	editor.putFloat(alarmValuePreferenceKey, (float)alarmValueInStorageUnit);
            }
            editor.apply();
        }
    }
	
	@Override
    public void onClick(View view) {
		if (alarmEnabledCheckBox.getId() == view.getId()) {
			alarmEnabledCheckboxChanged();
		}
    }

    private void alarmEnabledCheckboxChanged() {
        if (alarmEnabledCheckBox.isChecked()) {
            setValueTexViewVisibility(View.VISIBLE);
        } else {
            setValueTexViewVisibility(View.INVISIBLE);
        }
    }
	
	private void setAlarmValueUnitLabelBasedOnPreference() {
		String alarmValueUnitLabelTextViewValue = TemperatureUtil.getPreferredTemperatureUnit(getContext());
        alarmValueUnitLabelTextView.setText(alarmValueUnitLabelTextViewValue);
	}

	private boolean setAlarmEnabledCheckBoxValueBasedOnPreference() {
		SharedPreferences preferences = getSharedPreferences();
		
		String alarmEnabledPreferenceKey = getContext().getString(getAlarmEnabledPreferenceKey());
        boolean temperatureAlarmEnabledPreferenceValue = preferences.getBoolean(alarmEnabledPreferenceKey, false);
        alarmEnabledCheckBox.setChecked(temperatureAlarmEnabledPreferenceValue);
		return temperatureAlarmEnabledPreferenceValue;
	}

	private void setAlarmValueBasedOnPreference() {
		SharedPreferences preferences = getSharedPreferences();
		
		String preferenceKeyTeperatureAlarmValue = getContext().getString(getAlarmTemperaturePreferenceKey());
		Float temperatureAlarmPreferenceValue = preferences.getFloat(preferenceKeyTeperatureAlarmValue, 0);
		
		Double temperaturePreferenceValueInPreferenceUnit = TemperatureUtil.convertFromStorageUnitToPreferenceUnit(getContext(), temperatureAlarmPreferenceValue);
		alarmValueTextView.setText(temperaturePreferenceValueInPreferenceUnit.toString());
	}

    private void setValueTexViewVisibility(int visibility) {
        alarmValueTextView.setVisibility(visibility);
        alarmValueUnitLabelTextView.setVisibility(visibility);
    }
}