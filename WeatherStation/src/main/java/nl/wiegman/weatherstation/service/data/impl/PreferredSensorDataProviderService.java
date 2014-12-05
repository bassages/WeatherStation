package nl.wiegman.weatherstation.service.data.impl;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.device.DeviceSensorService;
import nl.wiegman.weatherstation.service.data.impl.random.RandomSensorDataValueService;
import nl.wiegman.weatherstation.service.data.impl.sensortag.SensorTagService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Provides sensor data from the preferred sensor to registered listeners
 */
public class PreferredSensorDataProviderService extends AbstractSensorDataProviderService implements SensorValueListener {
    private final String LOG_TAG = this.getClass().getSimpleName();

	private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
		
	private SensorDataProviderService sensorDataProviderService;
	
	private String sensorSourcePreferenceKey;
	
	@Override
	public void onCreate() {
		super.onCreate();
		sensorSourcePreferenceKey = getSensorSourcePreferenceKey();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
	}
	
	@Override
	public void activate() {
		startAndBindService(getPreferredSensorSourceClass(), sensorDataProviderServiceConnection);
		registerPreferenceListener();
	}

	@Override
	public void deactivate() {
		// Ignore
	}
	
	@Override
	public void valueUpdate(Context context, SensorType sensorType,	Double updatedValue) {
		publishSensorValueUpdate(sensorType, updatedValue);
	}

    private ServiceConnection sensorDataProviderServiceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName componentName, IBinder service) {    	
    		sensorDataProviderService = ((AbstractSensorDataProviderService.LocalBinder) service).getService();
    		sensorDataProviderService.addSensorValueListener(PreferredSensorDataProviderService.this, SensorType.values());
    		sensorDataProviderService.activate();
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName componentName) {
    		sensorDataProviderService = null;
    	}
    };
	
	private void startAndBindService(Class<?> serviceClass, ServiceConnection serviceConnection) {
    	Intent intent = new Intent(this, serviceClass);
    	ComponentName startedService = getApplicationContext().startService(intent);
    	if (startedService != null) {
        	boolean bindServiceSuccessFull = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        	if (!bindServiceSuccessFull) {
        		Log.e(LOG_TAG, "Binding to " + serviceClass.getSimpleName() + " was not successfull");
        	}    		
    	} else {
    		Log.e(LOG_TAG, "Starting service " + serviceClass.getSimpleName() + " was not successfull");
    	}
    }
	
    private Class<?> getPreferredSensorSourceClass() {
    	Class<?> sensorSourceClass = null;
    	
    	String sensorSourcePreference = getSensorSourcePreference();
    	if ("TI SensorTag".equals(sensorSourcePreference)) {
    		sensorSourceClass = SensorTagService.class;
    	} else if ("Device".equals(sensorSourcePreference)) {
    		sensorSourceClass = DeviceSensorService.class;
    	} else if ("Random".equals(sensorSourcePreference)) {
    		sensorSourceClass = RandomSensorDataValueService.class;
    	} else {
    		Log.e(LOG_TAG, "No valid sensor source preference: " + sensorSourcePreference);
    	}
		return sensorSourceClass;
	}
	
	private String getSensorSourcePreference() {
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
    	return sharedPreferences.getString(getSensorSourcePreferenceKey(), getDefaultSensorSource());
	}

	private String getDefaultSensorSource() {
		return getString(R.string.preference_sensor_source_default_value);
	}

	private String getSensorSourcePreferenceKey() {
		return getString(R.string.preference_sensor_source_key);
	}

	private void registerPreferenceListener() {
		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
    	preferenceListener = new PreferenceListener();	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
	}
	
	/**
	 * Handles changes in the preferred sensor source
	 */
	private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(sensorSourcePreferenceKey)) {
				removeSensorValueListener(PreferredSensorDataProviderService.this, SensorType.values());
			}
			sensorDataProviderService.deactivate();
			unbindService(sensorDataProviderServiceConnection);
			
			Class<?> preferredSensorSourceClass = getPreferredSensorSourceClass();
			startAndBindService(preferredSensorSourceClass, sensorDataProviderServiceConnection);
		}
	}
}
