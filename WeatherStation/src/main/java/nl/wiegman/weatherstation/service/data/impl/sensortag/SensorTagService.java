package nl.wiegman.weatherstation.service.data.impl.sensortag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor.BarometerGatt;
import nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor.GattSensor;
import nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor.HygrometerGatt;
import nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor.SensorData;
import nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor.ThermometerGatt;
import nl.wiegman.weatherstation.util.NamedThreadFactory;
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
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Provides sensor values from a TI sensortag
 */
public class SensorTagService extends AbstractSensorDataProviderService {
    private static final int REQUEST_ENABLE_BT = 1;

    private final String LOG_TAG = this.getClass().getSimpleName();
	
	private static final long PUBLISH_RATE_IN_MILLISECONDS = 10000;
	
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothLeService bluetoothLeService;
	
	private volatile BluetoothDevice connectedDevice;
	
	private BroadcastReceiver sensortagUpdateReceiver;
	
	private Intent serviceBindingIntent;
	
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
    public void onDestroy() {
        super.onDestroy();
        deactivate();
    }
    
	@Override
	public void activate() {
		Log.d(LOG_TAG, "activate");
		
		// Register the BroadcastReceiver to handle events from BluetoothAdapter and BluetoothLeService
        IntentFilter bluetoothEventFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        registerReceiver(bluetoothEventReceiver, bluetoothEventFilter);
		
		if (connectedDevice == null && checkBluetoothAvailable()) {
			startScanningForSensortag();
		}
	}

	@Override
	public void deactivate() {
		Log.d(LOG_TAG, "deactivate");
		
        releaseConnectionAndResources();
        unregisterReceiver(bluetoothEventReceiver);
	}

	private boolean checkBluetoothAvailable() {
		boolean available = false;
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    		BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
    		if (bluetoothAdapter != null) {
    			if (bluetoothAdapter.isEnabled()) {
    				available = true;
    			} else {
                    requestUserToEnableBluetooth();
    			}
    		} else {
    			broadCastAvailability(false, R.string.bluetooth_le_not_supported);
    		}
        } else {
        	broadCastAvailability(false, R.string.bluetooth_le_not_supported);
        }
        return available;
	}

    private void requestUserToEnableBluetooth() {
        Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(btIntent);
    }

    private void broadCastAvailability(boolean available, Integer messageStringId) {
        final Intent intent = new Intent(SensorDataProviderService.ACTION_AVAILABILITY_UPDATE);
        intent.putExtra(SensorDataProviderService.AVAILABILITY_UPDATE_AVAILABLE, available);
        intent.putExtra(SensorDataProviderService.AVAILABILITY_UPDATE_MESSAGEID, messageStringId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

    private void startScanningForSensortag() {
    	bluetoothAdapter = getBluetoothAdapter();
    	
        boolean scanningForBlueToothLeDevices = bluetoothAdapter.startLeScan(bluetoothLeScanCallback);
        if (!scanningForBlueToothLeDevices) {
            broadCastAvailability(false, R.string.start_le_scan_failed);
        } else {
        	broadCastAvailability(false, R.string.searching_for_sensortag);
        }
    }
	
    private void stopScanningForSensortag() {
    	if (bluetoothAdapter != null) {
    		bluetoothAdapter.stopLeScan(bluetoothLeScanCallback);
    	}
    }
    
    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            // Just connect to the first found device
        	stopScanningForSensortag();
            connectToDevice(device);
        }

        private void connectToDevice(BluetoothDevice deviceInfo) {
            connectedDevice = deviceInfo;
            startBluetoothLeService();
        }
    };
 
    private final ServiceConnection bluetoothLeServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	if (service instanceof BluetoothLeService.LocalBinder) {
            	bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!bluetoothLeService.initialize()) {
                    Log.e(LOG_TAG, "Unable to initialize BluetoothLeService");
                    broadCastAvailability(false, R.string.failed_to_connect_to_sensortag);
                } else {
                    Log.i(LOG_TAG, "BluetoothLeService connected");
                    if (connectedDevice != null) {
                    	boolean connectedSuccessfully = bluetoothLeService.connect(connectedDevice.getAddress());
                    	if (!connectedSuccessfully) {
                    		broadCastAvailability(false, R.string.failed_to_connect_to_sensortag);
                    	}
                    }
                }	
        	}
        }

        public void onServiceDisconnected(ComponentName componentName) {
        	// This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	bluetoothLeService = null;
        }
    };
    
    private void startBluetoothLeService() {
        serviceBindingIntent = new Intent(this, BluetoothLeService.class);
        startService(serviceBindingIntent);
        boolean serviceSucessfullyBound = bindService(serviceBindingIntent, bluetoothLeServiceConnection, Context.BIND_AUTO_CREATE);
        if (!serviceSucessfullyBound) {
            Log.e(LOG_TAG, "Failed to bind dind to BluetoothLeService");
            broadCastAvailability(false, R.string.failed_to_connect_to_sensortag);
        }
    }
    
    // Listens for broadcasted events from BlueTooth adapter and BluetoothLeService
    private final BroadcastReceiver bluetoothEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            	int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                bluetoothAdapterActionStateChanged(state);
            } else {
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    bluetoothLeServiceGattConnected(intent);
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    bluetoothLeServiceGattDisconnected();
                } else {
                    Log.w(LOG_TAG, "Unknown action: " + action);
                }
            }
        }

		private void bluetoothLeServiceGattDisconnected() {
			connectedDevice = null;
			Log.i(LOG_TAG, "Reconnect to sensortag");
			broadCastAvailability(false, R.string.connection_lost_trying_reconnect);
		    reconnect();
		}

		private void bluetoothLeServiceGattConnected(Intent intent) {
			int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				sensortagUpdateReceiver = new SensortagUpdateReceiver();
			    registerReceiver(sensortagUpdateReceiver, createSensorTagUpdateIntentFilter());
			    discoverServices();

			    Log.i(LOG_TAG, "Sucessfully connected to sensortag");
			} else {
			    Log.e(LOG_TAG, "Connecting to the sensortag failed. Status: " + status);
			    broadCastAvailability(false, R.string.failed_to_connect_to_sensortag);
			}
		}
		
	    private void discoverServices() {
	        if (BluetoothLeService.getBluetoothGatt().discoverServices()) {
	            Log.i(LOG_TAG, "Start service discovery");
	        } else {
	            Log.e(LOG_TAG, "Service discovery start failed");
	            broadCastAvailability(false, R.string.failed_to_connect_to_sensortag);
	        }
	    }

		private void bluetoothAdapterActionStateChanged(int state) {
			switch (state) {
			case BluetoothAdapter.STATE_ON:
			    activate();
			    break;
			case BluetoothAdapter.STATE_OFF:
				Log.i(LOG_TAG, "The bluetooth adapter was turned OFF");
				releaseConnectionAndResources();
			    break;
			default:
			    break;
			}
		}

	    private IntentFilter createSensorTagUpdateIntentFilter() {
	        IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
	        intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
	        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
	        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ);
	        return intentFilter;
	    }
    };

    private final class SensortagUpdateReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(LOG_TAG, "Services discovered");
                    startGattSensorDataUpdates();
                } else {
                    broadCastAvailability(false, R.string.sensortag_service_discovery_failed);
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
                onCharacteristicRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(LOG_TAG, "GATT error code: " + status);
            }
        }
    }

    private void onCharacteristicRead(String uuidStr, byte[] value, int status) {
    	if (value != null) {
            if (uuidStr.equals(BarometerGatt.UUID_CALIBRATION.toString())) {
                barometerGatt.processCalibrationResults(value);
            } else {
            	processChangedGattCharacteristic(uuidStr, value);
            }
    	} else {
			Log.w(LOG_TAG, "Sensor value is null");
    	}
    }

    public void onCharacteristicChanged(String uuidStr, byte[] value) {
    	if (value != null) {
    		processChangedGattCharacteristic(uuidStr, value);
    	} else {
			Log.w(LOG_TAG, "Sensor value is null");
    	}
    }
    
	private void processChangedGattCharacteristic(String uuidStr, byte[] value) {
		if (uuidStr.equals(thermometerGatt.getDataUuid().toString())) {
        	SensorData sensorData = thermometerGatt.convert(value);
        	publishSensorValueUpdate(SensorType.AmbientTemperature, sensorData.getX());
		} else if (uuidStr.equals(hygrometerGatt.getDataUuid().toString())) {
			SensorData sensorData = hygrometerGatt.convert(value);
        	publishSensorValueUpdate(SensorType.Humidity, sensorData.getX());
        } else if (uuidStr.equals(barometerGatt.getDataUuid().toString())) {
            SensorData sensorData = barometerGatt.convert(value);
            publishSensorValueUpdate(SensorType.AirPressure, sensorData.getX());
        } else {
            Log.e(LOG_TAG, "Unknown uuid: " + uuidStr);
        }
	}
    
    private void startGattSensorDataUpdates() {
        for (GattSensor gattSensor: gattSensors) {
            gattSensor.calibrate();
            gattSensor.enable();
        }
        periodicGattSensorUpdateRequestsExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("SensorTagUpdateRequestThread"));
        int startDelay = 500;
		PeriodicGattSensorUpdateRequester periodicGattSensorUpdateRequester = new PeriodicGattSensorUpdateRequester(gattSensors);
		periodicGattSensorUpdateRequestsExecutor.scheduleWithFixedDelay(periodicGattSensorUpdateRequester, startDelay, PUBLISH_RATE_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    private void reconnect() {
        releaseConnectionAndResources();
        activate();
    }

    private void releaseConnectionAndResources() {
        if (periodicGattSensorUpdateRequestsExecutor != null) {
            periodicGattSensorUpdateRequestsExecutor.shutdown();
            try {
    			periodicGattSensorUpdateRequestsExecutor.awaitTermination(5, TimeUnit.SECONDS);
    			periodicGattSensorUpdateRequestsExecutor = null;
    		} catch (InterruptedException e) {
    			Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
    		}
        }
    	
    	stopScanningForSensortag();

        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            unbindService(bluetoothLeServiceConnection);
            bluetoothLeService = null;
        }
        if (serviceBindingIntent != null) {
            stopService(serviceBindingIntent);
            serviceBindingIntent = null;
        }
        if (sensortagUpdateReceiver != null) {
            unregisterReceiver(sensortagUpdateReceiver);
            sensortagUpdateReceiver = null;
        }
        connectedDevice = null;
    }
    
	private BluetoothAdapter getBluetoothAdapter() {
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		return bluetoothManager.getAdapter();
	}

}
