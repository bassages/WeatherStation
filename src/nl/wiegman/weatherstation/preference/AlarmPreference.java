package nl.wiegman.weatherstation.preference;

import nl.wiegman.weatherstation.R;
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

public class AlarmPreference extends DialogPreference implements OnClickListener {

    private static final String LOG_TAG = AlarmPreference.class.getSimpleName();
    
    private CheckBox minumumTemperatureCheckBox;
    private TextView minimumTemperatureValueTextView;
    private TextView minimumTemperatureUnitLabelTextView;

    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_alarm);
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        SharedPreferences preferences = getSharedPreferences();
        
        minumumTemperatureCheckBox = (CheckBox) view.findViewById(R.id.preference_alarm_minimum_temerature_checkbox);
        minimumTemperatureValueTextView = (TextView) view.findViewById(R.id.preference_alarm_minimum_temperature_value);
        minimumTemperatureUnitLabelTextView = (TextView) view.findViewById(R.id.preference_alarm_minimum_temperature_unit_label);
        
        String temperatureUnitLabelValue = preferences.getString(getContext().getString(R.string.preference_temperature_unit_key), getContext().getString(R.string.preference_temperature_unit_default_value));
        minimumTemperatureUnitLabelTextView.setText(temperatureUnitLabelValue);
        
        String preferenceKey = getContext().getString(R.string.preference_alarm_minimum_temperature_enabled_key);
        boolean minimumTemperatureAlarmEnabledPreferenceValue = preferences.getBoolean(preferenceKey, false);
        minumumTemperatureCheckBox.setChecked(minimumTemperatureAlarmEnabledPreferenceValue);
        
        if (minimumTemperatureAlarmEnabledPreferenceValue) {
            minimumTemperatureValueTextView.setText("30.00");
        }
        
        minimumCheckboxChanged(minumumTemperatureCheckBox);
        minumumTemperatureCheckBox.setOnClickListener(this);
    }

    public void minimumCheckboxChanged(View view) {
        if (minumumTemperatureCheckBox.isChecked()) {
            setMinimumInputVisibility(View.VISIBLE);
        } else {
            setMinimumInputVisibility(View.INVISIBLE);
        }
    }

    private void setMinimumInputVisibility(int visibility) {
        minimumTemperatureValueTextView.setVisibility(visibility);
        minimumTemperatureUnitLabelTextView.setVisibility(visibility);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.i(LOG_TAG, "onDialogClosed(positiveResult=" + positiveResult + ")");
        
        if (positiveResult) {
            Editor editor = getEditor();
            
            String preferenceKeyMinimumTeperatureAlarmEnabled = getContext().getString(R.string.preference_alarm_minimum_temperature_enabled_key);
            String preferenceKeyMinimumTeperatureAlarmValue = getContext().getString(R.string.preference_alarm_minimum_temperature_value_key);
            
            boolean minimumTemperatureAlarmEnabled = minumumTemperatureCheckBox.isChecked();
            editor.putBoolean(preferenceKeyMinimumTeperatureAlarmEnabled, minimumTemperatureAlarmEnabled);
            if (minimumTemperatureAlarmEnabled) {
                CharSequence minimumTemperatureValueAsString = minimumTemperatureValueTextView.getText();
                float minimumTemperatureValueAsFloat = Float.parseFloat(minimumTemperatureValueAsString.toString());
                editor.putFloat(preferenceKeyMinimumTeperatureAlarmValue, minimumTemperatureValueAsFloat);
            }
            editor.commit();
        }
    }

    @Override
    public void onClick(View view) {
        minimumCheckboxChanged(view);
    }
}
