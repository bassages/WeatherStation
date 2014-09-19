package nl.wiegman.weatherstation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.bluetooth.BluetoothDeviceInfo;
import nl.wiegman.weatherstation.bluetooth.BluetoothLeService;
import nl.wiegman.weatherstation.fragment.SensorDataFragment;
import nl.wiegman.weatherstation.fragment.sensorvaluealarm.MaximumTemperatureAlarmHandler;
import nl.wiegman.weatherstation.fragment.sensorvaluealarm.MinimumTemperatureAlarmHandler;
import nl.wiegman.weatherstation.gattsensor.BarometerGatt;
import nl.wiegman.weatherstation.gattsensor.GattSensor;
import nl.wiegman.weatherstation.gattsensor.HygrometerGatt;
import nl.wiegman.weatherstation.gattsensor.SensorData;
import nl.wiegman.weatherstation.gattsensor.ThermometerGatt;
import nl.wiegman.weatherstation.sensorvaluelistener.BarometricPressureValueChangeListener;
import nl.wiegman.weatherstation.sensorvaluelistener.HumidityValueChangeListener;
import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final long SENSORS_REFRESH_RATE_IN_MILLISECONDS = TimeUnit.SECONDS.toMillis(30);
    
    // Requests to other activities
    private static final int REQUEST_TO_ENABLE_BLUETOOTHE_LE = 0;

    private Intent bluetoothLeServiceBindingIntent;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;
    
    private BroadcastReceiver sensortagUpdateReceiver;
    private BroadcastReceiver bluetoothEventReceiver;

    private volatile BluetoothDeviceInfo connectedDeviceInfo;
        
    private final List<TemperatureValueChangeListener> temperatureValueChangeListeners = new ArrayList<TemperatureValueChangeListener>();
    private final List<HumidityValueChangeListener> humidityValueChangeListeners = new ArrayList<HumidityValueChangeListener>();
    private final List<BarometricPressureValueChangeListener> barometricPressureChangeListeners = new ArrayList<BarometricPressureValueChangeListener>();

    private static final ThermometerGatt thermometerGatt = new ThermometerGatt();
    private static final HygrometerGatt hygrometerGatt = new HygrometerGatt();
    private static final BarometerGatt barometerGatt = new BarometerGatt();

    private ScheduledExecutorService periodicGattSensorUpdateRequestsExecutor;
    
    private static final List<GattSensor> gattSensors = new ArrayList<GattSensor>();
    static {
        gattSensors.add(barometerGatt);
        gattSensors.add(hygrometerGatt);
        gattSensors.add(thermometerGatt);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "onCreate(SavedInstanceState=" + savedInstanceState + ")");
        
        setContentView(R.layout.activity_main);
        
        SensorDataFragment sensorDataFragment = (SensorDataFragment) getFragmentManager().findFragmentById(R.id.activity_main);
        
        if (sensorDataFragment == null) {
            sensorDataFragment = new SensorDataFragment();
			getFragmentManager().beginTransaction().add(R.id.activity_main, sensorDataFragment).commit();
        }
		temperatureValueChangeListeners.add(sensorDataFragment);
		barometricPressureChangeListeners.add(sensorDataFragment);
		humidityValueChangeListeners.add(sensorDataFragment);
		
		MaximumTemperatureAlarmHandler maximumTemperatureAlarm = new MaximumTemperatureAlarmHandler(this);
		temperatureValueChangeListeners.add(maximumTemperatureAlarm);
		
		MinimumTemperatureAlarmHandler minimumTemperatureAlarm = new MinimumTemperatureAlarmHandler(this);
		temperatureValueChangeListeners.add(minimumTemperatureAlarm);
		
        // TODO: keep this?..
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "onStart()");
        
        startSensortagConnectionProcedure();
    }

	@Override
    protected void onStop() {
    	super.onStop();
        Log.i(LOG_TAG, "onStop()");
        
        stopScanningForSensortag();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy()");
        
        releaseConnectionAndResources();
    }
        
    private void startSensortagConnectionProcedure() {
    	setupBluetooth();
    	
        if (connectedDeviceInfo == null) {
            if (bluetoothAdapter.isEnabled()) {
                startScanningForSensortag();
            } else {
                requestUserToEnableBluetooth();
            }            
        } else {
        	Log.i(LOG_TAG, "Already connected to device " + connectedDeviceInfo.getBluetoothDevice());
        }
    }

    private void setupBluetooth() {
        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
    	
    	if (bluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();

            if (bluetoothAdapter == null) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
                finish();
            }    		
    	}
	}
    
	private void requestUserToEnableBluetooth() {
		// Request for the BlueTooth adapter to be turned on
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

		// The next call will (asynchronously) call onActivityResult(...) to
		// report weather or not the user enabled BlueTooth LE
		startActivityForResult(enableIntent, REQUEST_TO_ENABLE_BLUETOOTHE_LE);
	}
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case REQUEST_TO_ENABLE_BLUETOOTHE_LE:
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, R.string.bt_on, Toast.LENGTH_SHORT).show();
                startScanningForSensortag();
            } else {
                // User did not enable BlueTooth or an error occurred
                Toast.makeText(this, R.string.bt_not_on, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(LOG_TAG, "Unknown request code");
            break;
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

    private void startScanningForSensortag() {
        boolean scanningForBlueToothLeDevices = bluetoothAdapter.startLeScan(bluetoothLeScanCallback);
        if (!scanningForBlueToothLeDevices) {
            Toast.makeText(this, "Failed to search for sensortag", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void stopScanningForSensortag() {
        bluetoothAdapter.stopLeScan(bluetoothLeScanCallback);
    }

    // Listens for broadcasted events from BlueTooth adapter and BluetoothLeService
    private class BlueToothEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                bluetoothAdapterActionStateChanged();
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                bluetoothLeServiceGattConnected(context, intent);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                bluetoothLeServiceGattDisconnected(context, intent);
            } else {
                Log.w(LOG_TAG, "Unknown action: " + action);
            }
        }

		private void bluetoothLeServiceGattDisconnected(Context context, Intent intent) {
			connectedDeviceInfo = null;
			SensorDataFragment sensorDataFragment = (SensorDataFragment) getFragmentManager().findFragmentById(R.id.activity_main);
			sensorDataFragment.clearAllSensorValues();
			
			int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
			if (status == BluetoothGatt.GATT_SUCCESS) {
			    Log.i(LOG_TAG, "Disconnected, trying to reconnect...");
			    reconnect();
			} else {
			    Toast.makeText(context, "Disconnect failed. Status: " + status + status, Toast.LENGTH_LONG).show();
			}
		}

		private void bluetoothLeServiceGattConnected(Context context, Intent intent) {
			int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
			if (status == BluetoothGatt.GATT_SUCCESS) {
			    sensortagUpdateReceiver = new SensortagUpdateReceiver();
			    registerReceiver(sensortagUpdateReceiver, createSensorTagUpdateIntentFilter());
			    discoverServices();
			    
			    Log.i(LOG_TAG, "Sucessfully connected to sensortag");
			} else {
			    Toast.makeText(context, "Connecting to the sensortag failed. Status: " + status, Toast.LENGTH_LONG).show();
			    finish();
			}
		}

		private void bluetoothAdapterActionStateChanged() {
			switch (bluetoothAdapter.getState()) {
			case BluetoothAdapter.STATE_ON:
			    startBluetoothLeService();
			    break;
			case BluetoothAdapter.STATE_OFF:
			    finish();
			    break;
			default:
			    Log.w(LOG_TAG, "Action STATE CHANGED not processed: " + bluetoothAdapter.getState());
			    break;
			}
		}
    };
    
    private void reconnect() {
        releaseConnectionAndResources();
        startSensortagConnectionProcedure();
    }
    
    private void releaseConnectionAndResources() {
        stopScanningForSensortag();
        
        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            unbindService(serviceConnection);
            bluetoothLeService = null;
        }
        if (bluetoothEventReceiver != null) {
            unregisterReceiver(bluetoothEventReceiver);
            bluetoothEventReceiver = null;
        }
        if (bluetoothLeServiceBindingIntent != null) {
            stopService(bluetoothLeServiceBindingIntent);
            bluetoothLeServiceBindingIntent = null;
        }
        if (sensortagUpdateReceiver != null) {
            unregisterReceiver(sensortagUpdateReceiver);
            sensortagUpdateReceiver = null;
        }
        if (periodicGattSensorUpdateRequestsExecutor != null) {
            periodicGattSensorUpdateRequestsExecutor.shutdown();
            try {
    			periodicGattSensorUpdateRequestsExecutor.awaitTermination(5, TimeUnit.SECONDS);
    			periodicGattSensorUpdateRequestsExecutor = null;
    		} catch (InterruptedException e) {
    			Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
    		}        	
        }
        
        connectedDeviceInfo = null;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(LOG_TAG, "Unable to initialize BluetoothLeService");
                finish();
            } else {
                Log.i(LOG_TAG, "BluetoothLeService connected");
                if (connectedDeviceInfo != null) {
                    bluetoothLeService.connect(connectedDeviceInfo.getBluetoothDevice() .getAddress());
                }
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            Log.i(LOG_TAG, "BluetoothLeService disconnected");
        }
    };
    
    private void discoverServices() {
        if (BluetoothLeService.getBluetoothGatt().discoverServices()) {
            Log.i(LOG_TAG, "Start service discovery");
        } else {
            Log.e(LOG_TAG, "Service discovery start failed");
        }
    }

    private class SensortagUpdateReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(LOG_TAG, "Services discovered");

                    startSensorDataUpdates();
                    
                } else {
                    Toast.makeText(getApplication(), "Service discovery failed", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                onCharacteristicChanged(uuidStr, value);
            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                Log.d(LOG_TAG, "onCharacteristicWrite: " + uuidStr);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicsRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(LOG_TAG, "GATT error code: " + status);
            }
        }

        private void startSensorDataUpdates() {
            for (GattSensor gattSensor: gattSensors) {
                gattSensor.calibrate();
                gattSensor.enable();
            }
            periodicGattSensorUpdateRequestsExecutor = new ScheduledThreadPoolExecutor(1);
            int startDelay = 500;
			periodicGattSensorUpdateRequestsExecutor.scheduleWithFixedDelay(new PeriodicGattSensorUpdateRequester(), startDelay, SENSORS_REFRESH_RATE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
    };

    private final class PeriodicGattSensorUpdateRequester implements Runnable {
		@Override
		public void run() {
			for (final GattSensor gattSensor: gattSensors) {
				gattSensor.read();	
			}
		}
    }
    
    private void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
        if (uuidStr.equals(BarometerGatt.UUID_CALIBRATION.toString())) {
            barometerGatt.processCalibrationResults(value);
        } else {
        	onCharacteristicChanged(uuidStr, value);
        }
    }

    private static IntentFilter createSensorTagUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }

    /**
     * Handle changes in sensor values
     */
    public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
        if (uuidStr.equals(thermometerGatt.getDataUuid().toString())) {
        	SensorData sensorData = thermometerGatt.convert(rawValue);
            for (TemperatureValueChangeListener listener : temperatureValueChangeListeners) {
            	listener.temperatureChanged(this, sensorData.getX());            	
            }
        } else if (uuidStr.equals(hygrometerGatt.getDataUuid().toString())) {
            SensorData sensorData = hygrometerGatt.convert(rawValue);
            for (HumidityValueChangeListener listener : humidityValueChangeListeners) {
            	listener.humidityChanged(this, sensorData.getX());            	
            }
        } else if (uuidStr.equals(barometerGatt.getDataUuid().toString())) {
            SensorData sensorData = barometerGatt.convert(rawValue);
            for (BarometricPressureValueChangeListener listener : barometricPressureChangeListeners) {
            	listener.barometricPressureChanged(this, sensorData.getX());            	
            }
        } else {
            Log.e(LOG_TAG, "Unknown uuid: " + uuidStr);
        }
    }

    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            stopScanningForSensortag();
            connectToDevice(new BluetoothDeviceInfo(device, rssi));
        }
    };

    private void connectToDevice(BluetoothDeviceInfo deviceInfo) {
        connectedDeviceInfo = deviceInfo;

        // Register the BroadcastReceiver to handle events from BluetoothAdapter and BluetoothLeService
        IntentFilter bluetoothEventFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        
        bluetoothEventReceiver = new BlueToothEventReceiver();
        registerReceiver(bluetoothEventReceiver, bluetoothEventFilter);

        startBluetoothLeService();
    }

    private void startBluetoothLeService() {
        bluetoothLeServiceBindingIntent = new Intent(this, BluetoothLeService.class);
        startService(bluetoothLeServiceBindingIntent);
        boolean serviceSucessfullyBound = bindService(bluetoothLeServiceBindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (serviceSucessfullyBound)
            Log.d(LOG_TAG, "BluetoothLeService was sucessfully bound");
        else {
            Toast.makeText(this, "Bind to BluetoothLeService failed", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
