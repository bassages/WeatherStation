package nl.wiegman.weatherstation.fragment;

import java.text.DecimalFormat;

import nl.wiegman.weatherstation.MainActivity;
import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.sensorvaluelistener.AmbientTemperatureListener;
import nl.wiegman.weatherstation.sensorvaluelistener.BarometricPressureListener;
import nl.wiegman.weatherstation.sensorvaluelistener.HumidityListener;
import nl.wiegman.weatherstation.sensorvaluelistener.ObjectTemperatureListener;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show the latest sensor data
 */
public class SensorDataFragment extends Fragment implements AmbientTemperatureListener, ObjectTemperatureListener, HumidityListener, BarometricPressureListener {

	private static final String AMBIENT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY = "ambient_temperature";
	private static final String OBJECT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY = "object_temperature";
    private static final String BAROMETRIC_PRESSURE_SAVED_INSTANCE_STATE_KEY = "barometric_pressure";
	private static final String HUMIDITY_SAVED_INSTANCE_STATE_KEY = "humidity";

	private static final String LOG_TAG = SensorDataFragment.class.getSimpleName();
    
    private TextView temperatureValueTextView;
    private TextView temperatureUnitTextView;
    private Double ambientTemperatureInDegreeCelcius = null;
    private Double objectTemperatureInDegreeCelcius = null;
    
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextView;
    private Double humidity = null;
    
    private static final DecimalFormat barometricPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView barometricPressureValueTextView;
    private Double barometricPressure;
    
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    
	private String temperatureSourcePreferenceKey;
    private String temperatureSource;
	    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	temperatureSourcePreferenceKey = getTemperatureSourcePreferenceKey(getActivity());
    	temperatureSource = sharedPreferences.getString(temperatureSourcePreferenceKey, getDefaultTemperatureSource());
    	
		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		preferenceListener = new PreferenceListener();
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);

    	Log.i(LOG_TAG, "onCreate sensorDataFragment");  	
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_sensordata, container, false);
    	
    	temperatureUnitTextView = (TextView) rootView.findViewById(R.id.temperatureUnitText);
    	temperatureValueTextView = (TextView) rootView.findViewById(R.id.temperatureValue);
    	humidityValueTextView = (TextView) rootView.findViewById(R.id.humidityValue);
    	barometricPressureValueTextView = (TextView) rootView.findViewById(R.id.airPressureValue);
    	
    	if (savedInstanceState != null) {
    		restoreState(savedInstanceState);
    	}
    	
    	// Register as listener on various data
    	getMainActivity().addAmbientTemperatureListener(this);
    	getMainActivity().addObjectTemperatureListener(this);
    	getMainActivity().addBarometricPressureListener(this);
    	getMainActivity().addHumidityListener(this);
    	
    	return rootView;
    }

	private MainActivity getMainActivity() {
		return (MainActivity)getActivity();
	}

    @Override
    public void onStart() {
    	super.onStart();
    	applyPreferences();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    
    	if (ambientTemperatureInDegreeCelcius != null) {
    		outState.putDouble(AMBIENT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY, ambientTemperatureInDegreeCelcius);
    	}
    	if (objectTemperatureInDegreeCelcius != null) {
    		outState.putDouble(OBJECT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY, objectTemperatureInDegreeCelcius);
    	}
    	if (humidity != null) {
    		outState.putDouble(HUMIDITY_SAVED_INSTANCE_STATE_KEY, humidity);
    	}
    	if (barometricPressure != null) {
    		outState.putDouble(BAROMETRIC_PRESSURE_SAVED_INSTANCE_STATE_KEY, barometricPressure);
    	}
    }
    
	@Override
	public void ambientTemperatureUpdate(Context context, Double updatedTemperature) {
		if ("ambient".equalsIgnoreCase(temperatureSource)) {
			ambientTemperatureInDegreeCelcius = updatedTemperature;
			processTemperatureUpdate(updatedTemperature);	
		}
	}

	@Override
	public void objectTemperatureUpdate(Context context, Double updatedTemperature) {
		if ("object".equalsIgnoreCase(temperatureSource)) {
			objectTemperatureInDegreeCelcius = updatedTemperature;
			processTemperatureUpdate(updatedTemperature);
		}
	}
	
	private void processTemperatureUpdate(Double updatedTemperature) {
		if (temperatureValueTextView != null) {
			if (updatedTemperature == null) {
				temperatureValueTextView.setText(R.string.initial_temperature_value);            
			} else {
				double temperatureValueInPreferenceUnit = TemperatureUtil.convertFromStorageUnitToPreferenceUnit(getActivity(), updatedTemperature);
				String temperatureTextViewValue = TemperatureUtil.format(temperatureValueInPreferenceUnit);
				temperatureValueTextView.setText(temperatureTextViewValue);            
			}        	
		}
	}
	
	@Override
	public void humidityUpdate(Context context, Double updatedHumidity) {
		this.humidity = updatedHumidity;
		if (humidityValueTextView != null) {
			if (humidity == null) {
				humidityValueTextView.setText(R.string.initial_humidity_value);
			} else {
				String textviewValue = humidityValueTexviewFormat.format(humidity);
				humidityValueTextView.setText(textviewValue);
			}	
		}
    }
	
	@Override
	public void barometricPressureUpdate(Context context, Double updatedBarometricPressure) {
		this.barometricPressure = updatedBarometricPressure;
		if (barometricPressureValueTextView != null) {
	        if (barometricPressure == null) {
	            barometricPressureValueTextView.setText(R.string.initial_air_pressure_value);
	        } else {
	            String textviewValue = barometricPressureValueTextViewFormat.format(barometricPressure);
	            barometricPressureValueTextView.setText(textviewValue);
	        }
		}
	}
    
    public void clearAllSensorValues() {
    	ambientTemperatureUpdate(getActivity(), null);
    	objectTemperatureUpdate(getActivity(), null);
    	humidityUpdate(getActivity(), null);
    	barometricPressureUpdate(getActivity(), null);
    }
    
	private void restoreState(Bundle savedInstanceState) {
		ambientTemperatureInDegreeCelcius = savedInstanceState.getDouble(AMBIENT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY, Double.MIN_VALUE);
		if (ambientTemperatureInDegreeCelcius == Double.MIN_VALUE) {
			ambientTemperatureInDegreeCelcius = null;
		}
		objectTemperatureInDegreeCelcius = savedInstanceState.getDouble(OBJECT_TEMPERATURE_SAVED_INSTANCE_STATE_KEY, Double.MIN_VALUE);
		if (objectTemperatureInDegreeCelcius == Double.MIN_VALUE) {
			objectTemperatureInDegreeCelcius = null;
		}
		humidity = savedInstanceState.getDouble(HUMIDITY_SAVED_INSTANCE_STATE_KEY, Double.MIN_VALUE);
		if (humidity == Double.MIN_VALUE) {
			humidity = null;
		}
		barometricPressure = savedInstanceState.getDouble(BAROMETRIC_PRESSURE_SAVED_INSTANCE_STATE_KEY, Double.MIN_VALUE);
		if (barometricPressure == Double.MIN_VALUE) {
			barometricPressure = null;
		}		
		savedInstanceState.clear();
	}
    
    private void applyPreferences() {
        setTemperatureUnitLabelBasedOnPreference();
        
        ambientTemperatureUpdate(getActivity(), ambientTemperatureInDegreeCelcius);
        objectTemperatureUpdate(getActivity(), objectTemperatureInDegreeCelcius);
        humidityUpdate(getActivity(), humidity);
        barometricPressureUpdate(getActivity(), barometricPressure);
    }

    private void setTemperatureUnitLabelBasedOnPreference() {
        String preferredTemperatureUnit = TemperatureUtil.getPreferredTemperatureUnit(getActivity());
        temperatureUnitTextView.setText(preferredTemperatureUnit);
    }
    
	/**
	 * Handles changes in the temperature source preference
	 */
	private class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(temperatureSourcePreferenceKey)) {
				temperatureSource = sharedPreferences.getString(key, getDefaultTemperatureSource());
			}
		}
	}
	
	private String getTemperatureSourcePreferenceKey(Context context) {
		return context.getString(R.string.preference_temperature_source_key);
	}
	
	private String getDefaultTemperatureSource() {
		return getActivity().getString(R.string.preference_temperature_source_default_value);
	}
}
