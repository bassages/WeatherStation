package nl.wiegman.weatherstation.fragment;

import java.text.DecimalFormat;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.util.TemperatureUnit;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show the sensor data
 */
public class SensorDataFragment extends Fragment {
    private static final String LOG_TAG = SensorDataFragment.class.getSimpleName();
    
    private static final DecimalFormat temperatureValueTexviewFormat = new DecimalFormat("0.0;-0.0");
    private TextView temperatureValueTextView;
    private TextView temperatureUnitTextview;
    private Double temperatureInDegreeCelcius = null;
    
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextview;
    
    private static final DecimalFormat airPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView airPressureValueTextview;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	Log.i(LOG_TAG, "onCreate fragment");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_sensordata, container, false);
    	
    	temperatureUnitTextview = (TextView) rootView.findViewById(R.id.temperatureUnitText);
    	temperatureValueTextView = (TextView) rootView.findViewById(R.id.temperatureValue);
    	humidityValueTextview = (TextView) rootView.findViewById(R.id.humidityValue);
    	airPressureValueTextview = (TextView) rootView.findViewById(R.id.airPressureValue);
    	
    	return rootView;
    }

    @Override
    public void onStart() {
    	super.onStart();
    	applyPreferences();
    }

    public void setTemperature(Double temperatureInDegreeCelcius) {
        this.temperatureInDegreeCelcius = temperatureInDegreeCelcius;
        if (temperatureInDegreeCelcius == null) {
            temperatureValueTextView.setText(R.string.initial_temperature_value);
        } else {
            double temperatureValueInPreferenceUnit = TemperatureUnit.convertFromSiUnitToPreferenceUnit(getActivity(), temperatureInDegreeCelcius);
            String temperatureTextViewValue = temperatureValueTexviewFormat.format(temperatureValueInPreferenceUnit);
            temperatureValueTextView.setText(temperatureTextViewValue);            
        }
    }
    
    public void setAirPressure(Double airPressureInHectoPascal) {
        if (airPressureInHectoPascal == null) {
            airPressureValueTextview.setText(R.string.initial_air_pressure_value);
        } else {
            String textviewValue = airPressureValueTextViewFormat.format(airPressureInHectoPascal);
            airPressureValueTextview.setText(textviewValue);
        }
    }
    
    public void setHumidity(Double humidityPercentage) {
        if (humidityPercentage == null) {
            humidityValueTextview.setText(R.string.initial_humidity_value);            
        } else {
            String textviewValue = humidityValueTexviewFormat.format(humidityPercentage);
            humidityValueTextview.setText(textviewValue);
        }
    }
    
    public void clearAllSensorValues() {
        temperatureValueTextView.setText(R.string.initial_temperature_value);
        humidityValueTextview.setText(R.string.initial_humidity_value);
        airPressureValueTextview.setText(R.string.initial_air_pressure_value);
    }
    
    private void applyPreferences() {
        setTemperatureUnitLabelBasedOnPreference();
        setTemperature(temperatureInDegreeCelcius);
    }

    private void setTemperatureUnitLabelBasedOnPreference() {
        String preferredTemperatureUnit = TemperatureUnit.getPreferredTemperatureUnit(getActivity());
        temperatureUnitTextview.setText(preferredTemperatureUnit);
    }
}
