package nl.wiegman.weatherstation;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show the sensor data
 */
public class SensorDataFragment extends Fragment implements OnSharedPreferenceChangeListener {
    private SharedPreferences preferences;
    
    private static final DecimalFormat temperatureValueTexviewFormat = new DecimalFormat("0.0;-0.0");
    private TextView temperatureValueTextview;
    private TextView temperatureUnitTextview;
    private Double temperatureInDegreeCelcius = null;
    
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextview;
    private Double humidityPercentage = null;
    
    private static final DecimalFormat airPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView airPressureValueTextview;
    private Double airPressureInHectoPascal = null;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (!isAdded()) {
            return;
        }
        applyPreferences();
    }
    
    private void applyPreferences() {
        setTemperatureUnitBasedOnPreference();
        setTemperature(temperatureInDegreeCelcius);
    }

    private void setTemperatureUnitBasedOnPreference() {
        String preferredTemperatureUnit = getPreferredTemperatureUnit();
        temperatureUnitTextview.setText(preferredTemperatureUnit);
    }

    private String getPreferredTemperatureUnit() {
        String temperatureUnitPreferenceKey = getResources().getString(R.string.preference_temperature_unit_key);
        String temperatureUnitDefaultValue = getResources().getString(R.string.preference_temperature_unit_default_value);
        return preferences.getString(temperatureUnitPreferenceKey, temperatureUnitDefaultValue);
    }
     
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        
        temperatureUnitTextview = (TextView) rootView.findViewById(R.id.temperatureUnitText);
        temperatureValueTextview = (TextView) rootView.findViewById(R.id.temperatureValue);
        humidityValueTextview = (TextView) rootView.findViewById(R.id.humidityValue);
        airPressureValueTextview = (TextView) rootView.findViewById(R.id.airPressureValue);
        
        applyPreferences();
        
        return rootView;
    }
    
    public void setTemperature(Double temperatureInDegreeCelcius) {
        this.temperatureInDegreeCelcius = temperatureInDegreeCelcius;
        if (temperatureInDegreeCelcius == null) {
            temperatureValueTextview.setText(R.string.initial_temperature_value);
        } else {
            final String textviewValue;
            
            String fahrenheit = getResources().getString(R.string.temperature_unit_degree_fahrenheit);
            if (temperatureUnitTextview.getText().equals(fahrenheit)) {
                double temperatureInDegreeFahrenheit = convertCelciusToFahrenheit(temperatureInDegreeCelcius);
                textviewValue = temperatureValueTexviewFormat.format(temperatureInDegreeFahrenheit);
            } else {
                textviewValue = temperatureValueTexviewFormat.format(temperatureInDegreeCelcius);                
            }
            temperatureValueTextview.setText(textviewValue);            
        }
    }
    
    public void setAirPressure(Double airPressureInHectoPascal) {
        this.airPressureInHectoPascal = airPressureInHectoPascal;
        if (airPressureInHectoPascal == null) {
            airPressureValueTextview.setText(R.string.initial_air_pressure_value);
        } else {
            String textviewValue = airPressureValueTextViewFormat.format(airPressureInHectoPascal);
            airPressureValueTextview.setText(textviewValue);
        }
    }
    
    public void setHumidity(Double humidityPercentage) {
        this.humidityPercentage = humidityPercentage;
        if (humidityPercentage == null) {
            humidityValueTextview.setText(R.string.initial_humidity_value);            
        } else {
            String textviewValue = humidityValueTexviewFormat.format(humidityPercentage);
            humidityValueTextview.setText(textviewValue);
        }
    }
    
    public void clearAllSensorValues() {
        temperatureValueTextview.setText(R.string.initial_temperature_value);
        humidityValueTextview.setText(R.string.initial_humidity_value);
        airPressureValueTextview.setText(R.string.initial_air_pressure_value);
    }
    
    private double convertCelciusToFahrenheit(double degreeCelcius) {
        return 32 + (degreeCelcius * 9 / 5);
    }
}
