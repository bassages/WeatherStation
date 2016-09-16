package nl.wiegman.weatherstation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import nl.wiegman.weatherstation.fragment.SensorDataFragment;
import nl.wiegman.weatherstation.fragment.TemperatureHistoryFragment;
import nl.wiegman.weatherstation.service.alarm.SensorValueAlarmService;
import nl.wiegman.weatherstation.service.alarm.impl.MaximumTemperatureAlarm;
import nl.wiegman.weatherstation.service.alarm.impl.MinimumTemperatureAlarm;
import nl.wiegman.weatherstation.service.alarm.impl.SensorValueAlarmServiceImpl;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PreferredSensorDataProviderService;
import nl.wiegman.weatherstation.service.history.SensorValueHistoryService;
import nl.wiegman.weatherstation.service.history.impl.SensorValueHistoryServiceImpl;
import nl.wiegman.weatherstation.util.KeepScreenOnUtil;
import nl.wiegman.weatherstation.util.ThemeUtil;

/**
 * Main activity of the application
 */
public class MainActivity extends Activity {
    private final String LOG_TAG = this.getClass().getSimpleName();

    public static String ACTION_REQUEST_LOCATION_SERVICES = "nl.wiegman.weatherstation.service.data.REQUEST_LOCATION_SERVICES";

    private SensorDataProviderService sensorDataProviderService;
    private SensorValueHistoryService sensorValueHistoryService;
    private SensorValueAlarmService sensorValueAlarmService;

	private BroadcastReceiver locationServiceEnableRequestReciever;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private String preferenceThemeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		//noinspection ConstantConditions
		if (DevelopmentMode.getDevelopmentMode()) {
        	StrictMode.enableDefaults();
        }
        
		registerPreferenceListener();
		
		preferenceThemeKey = getApplicationContext().getString(R.string.preference_theme_key);
        ThemeUtil.setThemeFromPreferences(this);
        KeepScreenOnUtil.setKeepScreenOnFlagBasedOnPreference(this);
        
        setContentView(R.layout.activity_main);

		locationServiceEnableRequestReciever = new LocationServiceEnableRequestReciever();
        LocalBroadcastManager.getInstance(this).registerReceiver(locationServiceEnableRequestReciever,
                new IntentFilter(ACTION_REQUEST_LOCATION_SERVICES));

        startAndBindServices();

        if (savedInstanceState == null) {
        	showSensorData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");

    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        
		unbindServices();
    }

	private void showSensorData() {
		SensorDataFragment sensorDataFragment = new SensorDataFragment();

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.fragment_container, sensorDataFragment);
		fragmentTransaction.commit();
	}

    public void showHistory(View view) {
    	Fragment temperatureHistoryFragment = new TemperatureHistoryFragment();
    	Bundle arguments = new Bundle();
    	arguments.putString(SensorType.class.getSimpleName(), SensorType.AmbientTemperature.name());
    	temperatureHistoryFragment.setArguments(arguments);
    		
    	FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
    	fragmentTransaction.addToBackStack(null);
    	fragmentTransaction.replace(R.id.fragment_container, temperatureHistoryFragment);
   		fragmentTransaction.commit();	
	}
    
    private void startAndBindServices() {
        startAndBindService(PreferredSensorDataProviderService.class, sensorDataProviderServiceConnection);
        startAndBindService(SensorValueHistoryServiceImpl.class, sensorValueHistoryServiceConnection);
        startAndBindService(SensorValueAlarmServiceImpl.class, sensorValueAlarmServiceConnection);
	}

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
    
    private void unbindServices() {
        if (sensorDataProviderService != null) {
        	unbindService(sensorDataProviderServiceConnection);
        	sensorDataProviderService = null;
        }
        if (sensorValueHistoryService != null) {
        	unbindService(sensorValueHistoryServiceConnection);
        	sensorValueHistoryService = null;        	
        }
        if (sensorValueAlarmService != null) {
        	unbindService(sensorValueAlarmServiceConnection);
        	sensorValueAlarmService = null;        	
        }
		if (locationServiceEnableRequestReciever != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(locationServiceEnableRequestReciever);
			locationServiceEnableRequestReciever = null;
		}
    }

	private class LocationServiceEnableRequestReciever extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.do_you_want_to_enable_location_services)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();
		}
	}

	private ServiceConnection sensorDataProviderServiceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName componentName, IBinder service) {    	
    		sensorDataProviderService = ((AbstractSensorDataProviderService.LocalBinder) service).getService();
    		sensorDataProviderService.activate();
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName componentName) {
    		sensorDataProviderService = null;
    	}
    };

    private ServiceConnection sensorValueHistoryServiceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName componentName, IBinder service) {    	
    		sensorValueHistoryService = ((SensorValueHistoryServiceImpl.LocalBinder) service).getService();
    		sensorValueHistoryService.activate();
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName componentName) {
    		sensorValueHistoryService = null;
    	}
    };

    private ServiceConnection sensorValueAlarmServiceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName componentName, IBinder service) {    	
    		sensorValueAlarmService = ((SensorValueAlarmServiceImpl.LocalBinder) service).getService();
    		sensorValueAlarmService.activate(new MinimumTemperatureAlarm(), new MaximumTemperatureAlarm());
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName componentName) {
    		sensorValueAlarmService = null;
    	}
    };
	
	private void registerPreferenceListener() {
		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
    	preferenceListener = new PreferenceListener();	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
	}
	
	/**
	 * Handles changes in the preferences
	 */
	private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			String preferenceKeepScreenOnPreferenceKey = getApplicationContext().getString(R.string.preference_keep_screen_on_key);
			
			if (key.equals(preferenceThemeKey)) {
				recreate();
			} else if (key.equals(preferenceKeepScreenOnPreferenceKey)) {
				KeepScreenOnUtil.setKeepScreenOnFlagBasedOnPreference(MainActivity.this);
			}
		}
	}
}
