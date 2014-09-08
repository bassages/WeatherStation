package nl.wiegman.weatherstation.preference;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.util.TemperatureUnit;
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

    private static final String LOG_TAG = TemperatureAlarmPreference.class.getSimpleName();
    
    private CheckBox temperatureAlarmEnabledCheckBox;
    private TextView temperatureValueTextView;
    private TextView temperatureUnitLabelTextView;

    /**
     * Constructor
     */
    public TemperatureAlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_alarm);
    }
    
	protected abstract int getAlarmEnabledPreferenceKey();

	protected abstract int getAlarmTemperaturePreferenceKey();
    
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		temperatureAlarmEnabledCheckBox = (CheckBox) view.findViewById(R.id.preference_alarm_enabled_checkbox);
		temperatureValueTextView = (TextView) view.findViewById(R.id.preference_alarm_temperature_value_textview);
		temperatureUnitLabelTextView = (TextView) view.findViewById(R.id.preference_alarm_temperature_unit_label);
		
		setTemperatureUnitLabelBasedOnPreference();
		
		boolean alarmEnabledPreferenceValue = setAlarmEnabledCheckBoxValueBasedOnPreference();
		if (alarmEnabledPreferenceValue) {
			setAlarmValueBasedOnPreference();
		}
		
		temperatureAlarmEnabledCheckboxChanged();
		temperatureAlarmEnabledCheckBox.setOnClickListener(this);
	}

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.i(LOG_TAG, "onDialogClosed(positiveResult=" + positiveResult + ")");
        
        if (positiveResult) {
            Editor editor = getEditor();
            
            String temperatureAlarmEnabledPreferenceKey = getContext().getString(getAlarmEnabledPreferenceKey());
            String temperatureAlarmValuePreferenceKey = getContext().getString(getAlarmTemperaturePreferenceKey());
            
            boolean temperatureAlarmEnabled = temperatureAlarmEnabledCheckBox.isChecked();
            Log.i(LOG_TAG, "Setting preference " + temperatureAlarmEnabledPreferenceKey + " to " + temperatureAlarmEnabled);
            editor.putBoolean(temperatureAlarmEnabledPreferenceKey, temperatureAlarmEnabled);

            CharSequence temperatureValueAsString = temperatureValueTextView.getText();
            if (temperatureAlarmEnabled && isNotEmpty(temperatureValueAsString)) {
               	float minimumTemperatureValueAsFloat = Float.parseFloat(temperatureValueAsString.toString());
               	
               	double temperatureValueSi = TemperatureUnit.convertFromPreferenceUnitToSiUnit(getContext(), minimumTemperatureValueAsFloat);
               	
               	Log.i(LOG_TAG, "Setting preference " + temperatureAlarmValuePreferenceKey + " to " + temperatureValueSi);
               	editor.putFloat(temperatureAlarmValuePreferenceKey, (float)temperatureValueSi);
            }
            editor.commit();
        }
    }
	
	@Override
    public void onClick(View view) {
		if (temperatureAlarmEnabledCheckBox.getId() == view.getId()) {
			temperatureAlarmEnabledCheckboxChanged();
		}
    }

    private void temperatureAlarmEnabledCheckboxChanged() {
        if (temperatureAlarmEnabledCheckBox.isChecked()) {
            setValueTexViewVisibility(View.VISIBLE);
        } else {
            setValueTexViewVisibility(View.INVISIBLE);
        }
    }
	
	private void setTemperatureUnitLabelBasedOnPreference() {
		String temperatureUnitLabelTextViewValue = TemperatureUnit.getPreferredTemperatureUnit(getContext());
        temperatureUnitLabelTextView.setText(temperatureUnitLabelTextViewValue);
	}

	private boolean setAlarmEnabledCheckBoxValueBasedOnPreference() {
		SharedPreferences preferences = getSharedPreferences();
		
		String alarmEnabledPreferenceKey = getContext().getString(getAlarmEnabledPreferenceKey());
        boolean temperatureAlarmEnabledPreferenceValue = preferences.getBoolean(alarmEnabledPreferenceKey, false);
        temperatureAlarmEnabledCheckBox.setChecked(temperatureAlarmEnabledPreferenceValue);
		return temperatureAlarmEnabledPreferenceValue;
	}

	private void setAlarmValueBasedOnPreference() {
		SharedPreferences preferences = getSharedPreferences();
		
		String preferenceKeyTeperatureAlarmValue = getContext().getString(getAlarmTemperaturePreferenceKey());
		Float temperatureAlarmPreferenceValue = preferences.getFloat(preferenceKeyTeperatureAlarmValue, 0);
		
		Double temperaturePreferenceValueInPreferenceUnit = TemperatureUnit.convertFromSiUnitToPreferenceUnit(getContext(), temperatureAlarmPreferenceValue);
		temperatureValueTextView.setText(temperaturePreferenceValueInPreferenceUnit.toString());
	}

    private void setValueTexViewVisibility(int visibility) {
        temperatureValueTextView.setVisibility(visibility);
        temperatureUnitLabelTextView.setVisibility(visibility);
    }
    
	private boolean isNotEmpty(CharSequence temperatureValueAsString) {
    	return temperatureValueAsString != null && !"".equals(temperatureValueAsString.toString().trim());
	}
}