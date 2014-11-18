package nl.wiegman.weatherstation.fragment;

import java.text.DecimalFormat;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.SensorDataProviderAvailabilityBroadcast;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import nl.wiegman.weatherstation.util.ThemeUtil;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fragment to show the latest sensor data
 */
public class SensorDataFragment extends Fragment implements SensorValueListener {

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

    private String themePreferenceKey;
	private String temperatureSourcePreferenceKey;
    private String preferredTemperatureSource;

    private SensorDataProviderService dataProviderService;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	temperatureSourcePreferenceKey = getTemperatureSourcePreferenceKey(getActivity().getApplicationContext());
    	preferredTemperatureSource = sharedPreferences.getString(temperatureSourcePreferenceKey, getDefaultTemperatureSource());
    	themePreferenceKey = getActivity().getApplicationContext().getString(R.string.preference_theme_key);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		preferenceListener = new PreferenceListener();
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		Bundle arguments = getArguments();
		
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(sensorDataProviderAvailabilityReceiver,
      	      new IntentFilter(SensorDataProviderAvailabilityBroadcast.ACTION_AVAILABILITY_UPDATE));
		
		String sensorDataProviderServiceClassName = arguments.getString(SensorDataProviderService.class.getSimpleName());
		try {
			Class<?> sensorDataProviderServiceClass = Class.forName(sensorDataProviderServiceClassName);
			bindSensorDataProviderService(sensorDataProviderServiceClass);
		} catch (ClassNotFoundException e) {
			Log.e(LOG_TAG, "Unable to bind SensorDataProviderService of class " + sensorDataProviderServiceClassName);
		}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_sensordata, container, false);
    	
    	temperatureUnitTextView = (TextView) rootView.findViewById(R.id.temperatureUnitText);
    	temperatureValueTextView = (TextView) rootView.findViewById(R.id.temperatureValue);
    	humidityValueTextView = (TextView) rootView.findViewById(R.id.humidityValue);
    	barometricPressureValueTextView = (TextView) rootView.findViewById(R.id.airPressureValue);
    	
    	setImagesBasedOnTheme(rootView);
    	
    	if (savedInstanceState != null) {
    		restoreState(savedInstanceState);
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
    public void onDestroy() {
    	super.onDestroy();

    	unregisterAsDataListener();

    	getActivity().unbindService(sensorDataProviderServiceConnection);
    	
    	LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(sensorDataProviderAvailabilityReceiver);
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

	@Override
	public void valueUpdate(Context context, SensorType sensortype, Double updatedValue) {
		switch (sensortype) {
		case AmbientTemperature:
			ambientTemperatureUpdate(context, updatedValue);
			break;
		case ObjectTemperature:
			objectTemperatureUpdate(context, updatedValue);
			break;
		case Humidity:
			humidityUpdate(context, updatedValue);
			break;
		case AirPressure:
			barometricPressureUpdate(context, updatedValue);
			break;
		default:
			Log.w(LOG_TAG, "Unknown sensorType: " + sensortype.name());
		}
	}
    
    private void bindSensorDataProviderService(Class<?> sensorDataProviderServiceClassToStart) {	
    	Intent intent = new Intent(this.getActivity(), sensorDataProviderServiceClassToStart);
    	boolean bindServiceSuccessFull = getActivity().bindService(intent, sensorDataProviderServiceConnection, Context.BIND_AUTO_CREATE);
    	if (!bindServiceSuccessFull) {
    		Log.e(LOG_TAG, "Binding to SensorDataProviderService was not successfull");
    	}
    }
    
	private void setImagesBasedOnTheme(View rootView) {
		ImageView humidityImageView = (ImageView)rootView.findViewById(R.id.humidityImage);
    	ImageView airPressureImageView = (ImageView)rootView.findViewById(R.id.airPressureImage);
    	ImageView temperatureImageView = (ImageView)rootView.findViewById(R.id.temperatureImage);

    	String themeFromPreferences = ThemeUtil.getThemeFromPreferences(getActivity().getApplicationContext());
    	if (themeFromPreferences.equals("Dark")) {
    		humidityImageView.setImageResource(R.drawable.humidity_white);
    		airPressureImageView.setImageResource(R.drawable.airpressure_white);
    		temperatureImageView.setImageResource(R.drawable.temperature_white);
    	} else {
    		humidityImageView.setImageResource(R.drawable.humidity_black);
    		airPressureImageView.setImageResource(R.drawable.airpressure_black);
    		temperatureImageView.setImageResource(R.drawable.temperature_black);
    	}
	}

	private void ambientTemperatureUpdate(Context context, Double updatedTemperature) {
		if ("ambient".equalsIgnoreCase(preferredTemperatureSource)) {
			ambientTemperatureInDegreeCelcius = updatedTemperature;
			processTemperatureUpdate(updatedTemperature);
		}
	}
	
	private void objectTemperatureUpdate(Context context, Double updatedTemperature) {
		if ("object".equalsIgnoreCase(preferredTemperatureSource)) {
			objectTemperatureInDegreeCelcius = updatedTemperature;
			processTemperatureUpdate(updatedTemperature);
		}
	}

	private void processTemperatureUpdate(final Double updatedTemperature) {
		if (temperatureValueTextView != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (updatedTemperature == null) {
						temperatureValueTextView.setText(R.string.initial_temperature_value);
					} else {
						double temperatureValueInPreferenceUnit = TemperatureUtil.convertFromStorageUnitToPreferenceUnit(getActivity(), updatedTemperature);
						String temperatureTextViewValue = TemperatureUtil.format(temperatureValueInPreferenceUnit);
						temperatureValueTextView.setText(temperatureTextViewValue);
					}
				}				
			});
		}
	}

	private void humidityUpdate(Context context, Double updatedHumidity) {
		this.humidity = updatedHumidity;
		if (humidityValueTextView != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (humidity == null) {
						humidityValueTextView.setText(R.string.initial_humidity_value);
					} else {
						String textviewValue = humidityValueTexviewFormat.format(humidity);
						humidityValueTextView.setText(textviewValue);
					}
				}
			});
		}
    }

	private void barometricPressureUpdate(Context context, Double updatedBarometricPressure) {
		this.barometricPressure = updatedBarometricPressure;
		if (barometricPressureValueTextView != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
			        if (barometricPressure == null) {
			            barometricPressureValueTextView.setText(R.string.initial_air_pressure_value);
			        } else {
			            String textviewValue = barometricPressureValueTextViewFormat.format(barometricPressure);
			            barometricPressureValueTextView.setText(textviewValue);
			        }
				}
			});
		}
	}

    public void clearAllSensorValues() {
    	for (SensorType sensorType : SensorType.values()) {
    		valueUpdate(getActivity(), sensorType, null);
    	}
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

        valueUpdate(getActivity(), SensorType.AmbientTemperature, ambientTemperatureInDegreeCelcius);
        valueUpdate(getActivity(), SensorType.ObjectTemperature, objectTemperatureInDegreeCelcius);
        valueUpdate(getActivity(), SensorType.Humidity, humidity);
        valueUpdate(getActivity(), SensorType.AirPressure, barometricPressure);
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
				preferredTemperatureSource = sharedPreferences.getString(key, getDefaultTemperatureSource());
			} else if (key.equals(themePreferenceKey)) {
				getActivity().recreate();
			}
		}
	}

	private BroadcastReceiver sensorDataProviderAvailabilityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean available = intent.getBooleanExtra(SensorDataProviderAvailabilityBroadcast.AVAILABILITY_UPDATE_AVAILABLE, false);
			if (!available) {
				clearAllSensorValues();
			}
		}
	};
	
	private String getTemperatureSourcePreferenceKey(Context context) {
		return context.getString(R.string.preference_temperature_source_key);
	}

	private String getDefaultTemperatureSource() {
		return getActivity().getString(R.string.preference_temperature_source_default_value);
	}

	private ServiceConnection sensorDataProviderServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			dataProviderService = ((AbstractSensorDataProviderService.LocalBinder) service).getService();
			registerAsDataListener();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			unregisterAsDataListener();
		}
	};
	
	private void registerAsDataListener() {
		for (SensorType sensorType : SensorType.values()) {
			dataProviderService.addSensorValueListener(this, sensorType);			
		}
	}
	
	private void unregisterAsDataListener() {
		for (SensorType sensorType : SensorType.values()) {
			dataProviderService.removeSensorValueListener(this, sensorType);			
		}
	}
}
