package nl.wiegman.weatherstation.fragment;

import java.text.DecimalFormat;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.sensorvaluelistener.BarometricPressureValueChangeListener;
import nl.wiegman.weatherstation.sensorvaluelistener.HumidityValueChangeListener;
import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show the sensor data
 */
public class SensorDataFragment extends Fragment implements TemperatureValueChangeListener, HumidityValueChangeListener, BarometricPressureValueChangeListener {
    private static final String LOG_TAG = SensorDataFragment.class.getSimpleName();
    
    private TextView temperatureValueTextView;
    private TextView temperatureUnitTextView;
    private Double temperatureInDegreeCelcius = null;
    
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextView;
    private Double humidity = null;
    
    private static final DecimalFormat barometricPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView barometricPressureValueTextView;
    private Double barometricPressure;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	Log.i(LOG_TAG, "onCreate fragment");    	
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_sensordata, container, false);
    	
    	temperatureUnitTextView = (TextView) rootView.findViewById(R.id.temperatureUnitText);
    	temperatureValueTextView = (TextView) rootView.findViewById(R.id.temperatureValue);
    	humidityValueTextView = (TextView) rootView.findViewById(R.id.humidityValue);
    	barometricPressureValueTextView = (TextView) rootView.findViewById(R.id.airPressureValue);
    	
    	if (savedInstanceState != null) {
    		temperatureInDegreeCelcius = savedInstanceState.getDouble("temperature");
    		humidity = savedInstanceState.getDouble("humidity");
    		barometricPressure = savedInstanceState.getDouble("barometric_pressure");
    	}
    	
    	return rootView;
    }

    @Override
    public void onStart() {
    	super.onStart();
    	applyPreferences();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putDouble("temperature", temperatureInDegreeCelcius);
    	outState.putDouble("humidity", humidity);
    	outState.putDouble("barometric_pressure", barometricPressure);
    }
    
	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
        this.temperatureInDegreeCelcius = updatedTemperature;
        if (temperatureInDegreeCelcius == null) {
            temperatureValueTextView.setText(R.string.initial_temperature_value);            
        } else {
            double temperatureValueInPreferenceUnit = TemperatureUtil.convertFromStorageUnitToPreferenceUnit(getActivity(), temperatureInDegreeCelcius);
            String temperatureTextViewValue = TemperatureUtil.format(temperatureValueInPreferenceUnit);
            temperatureValueTextView.setText(temperatureTextViewValue);            
        }
	}
    
	@Override
	public void humidityChanged(Context context, Double updatedHumidity) {
		this.humidity = updatedHumidity;
        if (humidity == null) {
            humidityValueTextView.setText(R.string.initial_humidity_value);
        } else {
            String textviewValue = humidityValueTexviewFormat.format(humidity);
            humidityValueTextView.setText(textviewValue);
        }	
    }
	
	@Override
	public void barometricPressureChanged(Context context, Double updatedBarometricPressure) {
		this.barometricPressure = updatedBarometricPressure;
        if (barometricPressure == null) {
            barometricPressureValueTextView.setText(R.string.initial_air_pressure_value);
        } else {
            String textviewValue = barometricPressureValueTextViewFormat.format(barometricPressure);
            barometricPressureValueTextView.setText(textviewValue);
        }			
	}
    
    public void clearAllSensorValues() {
        temperatureValueTextView.setText(R.string.initial_temperature_value);
        humidityValueTextView.setText(R.string.initial_humidity_value);
        barometricPressureValueTextView.setText(R.string.initial_air_pressure_value);
    }
    
    private void applyPreferences() {
        setTemperatureUnitLabelBasedOnPreference();
        
        temperatureChanged(getActivity(), temperatureInDegreeCelcius);
        humidityChanged(getActivity(), humidity);
        barometricPressureChanged(getActivity(), barometricPressure);
    }

    private void setTemperatureUnitLabelBasedOnPreference() {
        String preferredTemperatureUnit = TemperatureUtil.getPreferredTemperatureUnit(getActivity());
        temperatureUnitTextView.setText(preferredTemperatureUnit);
    }
}
