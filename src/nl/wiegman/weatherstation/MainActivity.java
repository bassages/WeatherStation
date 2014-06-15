package nl.wiegman.weatherstation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.wiegman.weatherstation.gattsensor.BarometerGatt;
import nl.wiegman.weatherstation.gattsensor.GattSensor;
import nl.wiegman.weatherstation.gattsensor.HygrometerGatt;
import nl.wiegman.weatherstation.gattsensor.SensorData;
import nl.wiegman.weatherstation.gattsensor.ThermometerGatt;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // Tag for logging
    private static final String TAG = MainActivity.class.getSimpleName();

    // Requests to other activities
    private static final int REQUEST_TO_ENABLE_BLUETOOTHE_LE = 0;

    private Context thisContext = this;

    private Intent bindIntent;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;
    
    private BroadcastReceiver sensortagUpdateReceiver;
    private BroadcastReceiver bluetoothEventReceiver;

    private volatile BleDeviceInfo connectedDeviceInfo;
    
    private static final ThermometerGatt thermometerGatt = new ThermometerGatt();
    private static final DecimalFormat temperatureValueTexviewFormat = new DecimalFormat("0.0;-0.0");
    private TextView temperatureValueTextview = null;
    
    private static final HygrometerGatt hygrometerGatt = new HygrometerGatt();
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextview = null;

    private static final BarometerGatt barometerGatt = new BarometerGatt();
    private static final DecimalFormat airPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView airPressureValueTextview = null;
    
    private static final List<GattSensor> gattSensors = new ArrayList<GattSensor>();
    static {
        gattSensors.add(barometerGatt);
        gattSensors.add(hygrometerGatt);
        // gattSensors.add(thermometerGatt); // Temperature is read from barometer temperature sensor
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }

        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to BluetoothAdapter through BluetoothManager.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
         
        temperatureValueTextview = (TextView) findViewById(R.id.temperatureValue);
        humidityValueTextview = (TextView) findViewById(R.id.humidityValue);
        airPressureValueTextview = (TextView) findViewById(R.id.airPressureValue);
        
        if (bluetoothAdapter.isEnabled()) {
            startScanningForSensortag();
        } else {
            // Request for the BlueTooth adapter to be turned on
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            // The next call will (asynchronously) call onActivityResult(...) to
            // report weather or not the user enabled BlueTooth LE
            startActivityForResult(enableIntent, REQUEST_TO_ENABLE_BLUETOOTHE_LE);
        }
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
            Log.e(TAG, "Unknown request code");
            break;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart()");
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
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseResources();
    }

    private void startScanningForSensortag() {
        boolean scanningForBlueToothLeDevices = bluetoothAdapter.startLeScan(bluetoothLeScanCallback);
        if (!scanningForBlueToothLeDevices) {
            Toast.makeText(this, "Failed to search for sensortag", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Searching for sensortag...", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopScanningForBluetoothLeDevices() {
        bluetoothAdapter.stopLeScan(bluetoothLeScanCallback);
    }

    // Listens for broadcasted events from BlueTooth adapter and
    // BluetoothLeService
    private class BlueToothEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth adapter state change
                switch (bluetoothAdapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    startBluetoothLeService();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    Toast.makeText(context, R.string.app_closing, Toast.LENGTH_LONG).show();
                    finish();
                    break;
                default:
                    Log.w(TAG, "Action STATE CHANGED not processed ");
                    break;
                }
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sensortagUpdateReceiver = new SensortagUpdateReceiver();
                    registerReceiver(sensortagUpdateReceiver, createSensorTagUpdateIntentFilter());
                    discoverServices();
                    
                    Toast.makeText(thisContext, "Sucessfully connected to sensortag", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(thisContext, "Connecting to the sensortag failed. Status: " + status, Toast.LENGTH_LONG).show();
                    finish();
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connectedDeviceInfo = null;
                clearAllSensorValues();
                
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Disconnected, trying to reconnect...");
                    reconnect();
                } else {
                    Toast.makeText(thisContext, "Disconnect failed. Status: " + status + status, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w(TAG, "Unknown action: " + action);
            }
        }
    };
    
    private void reconnect() {
        releaseResources();
        startScanningForSensortag();
    }
    
    private void releaseResources() {
        stopScanningForBluetoothLeDevices();
        
        if (bluetoothLeService != null) {
            bluetoothLeService.close();
            unbindService(serviceConnection);
            bluetoothLeService = null;
        }
        if (bluetoothEventReceiver != null) {
            unregisterReceiver(bluetoothEventReceiver);
            bluetoothEventReceiver = null;
        }
        if (bindIntent != null) {
            stopService(bindIntent);
            bindIntent = null;
        }
        if (sensortagUpdateReceiver != null) {
            unregisterReceiver(sensortagUpdateReceiver);
            sensortagUpdateReceiver = null;
        }  
    }

    // Code to manage Service life cycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize BluetoothLeService");
                finish();
            } else {
                Log.i(TAG, "BluetoothLeService connected");
                if (connectedDeviceInfo != null) {
                    bluetoothLeService.connect(connectedDeviceInfo.getBluetoothDevice() .getAddress());
                }
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            Log.i(TAG, "BluetoothLeService disconnected");
        }
    };

    private void enableSensor(GattSensor sensor, boolean enable) {
        UUID servUuid = sensor.getService();
        UUID confUuid = sensor.getConfig();

        BluetoothGattService serv = BluetoothLeService.getBluetoothGatt().getService(servUuid);
        BluetoothGattCharacteristic characteristic = serv.getCharacteristic(confUuid);
        byte value = enable ? sensor.getEnableSensorCode() : sensor.getDisableSensorCode();
        
        BluetoothLeService.getInstance().initiateWriteCharacteristic(characteristic, value);
    }
    
    private void enableNotifications(GattSensor sensor, boolean enable) {
        UUID servUuid = sensor.getService();
        UUID dataUuid = sensor.getData();
        
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(servUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(dataUuid);

        boolean setNotificationCharacteristicSuccesfull = BluetoothLeService.getInstance().initiateNotificationCharacteristic(characteristic, enable);
        if (!setNotificationCharacteristicSuccesfull) {
            Log.e(TAG, "Failed to setNotificationCharacteristic");
        }
    }
    
    private void discoverServices() {
        if (BluetoothLeService.getBluetoothGatt().discoverServices()) {
            Log.i(TAG, "Start service discovery");
        } else {
            Log.e(TAG, "Service discovery start failed");
        }
    }

    private class SensortagUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Services discovered");

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
                Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicsRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT error code: " + status);
            }
        }

        private void startSensorDataUpdates() {
            for (GattSensor gattSensor: gattSensors) {
                gattSensor.calibrate();
                enableSensor(gattSensor, true);
                enableNotifications(gattSensor, true);
            }
        }
    };

    private void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
        if (uuidStr.equals(BarometerGatt.UUID_CALIBRATION.toString())) {
            barometerGatt.processCalibrationResults(value);
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
        SensorData sensorData;
        String textviewValue;
        
        if (uuidStr.equals(thermometerGatt.getData().toString())) {
            sensorData = thermometerGatt.convert(rawValue);
            textviewValue = temperatureValueTexviewFormat.format(sensorData.x);
            temperatureValueTextview.setText(textviewValue);
            
        } else if (uuidStr.equals(hygrometerGatt.getData().toString())) {
            sensorData = hygrometerGatt.convert(rawValue);
            textviewValue = humidityValueTexviewFormat.format(sensorData.x);
            humidityValueTextview.setText(textviewValue);
            
        } else if (uuidStr.equals(barometerGatt.getData().toString())) {
            sensorData = barometerGatt.convert(rawValue);
            textviewValue = airPressureValueTextViewFormat.format(sensorData.x);
            airPressureValueTextview.setText(textviewValue);
            
            textviewValue = temperatureValueTexviewFormat.format(sensorData.y);
            temperatureValueTextview.setText(textviewValue);
            
        } else {
            Log.w(TAG, "Unknown uuid: " + uuidStr);
        }
    }
    
    private void clearAllSensorValues() {
        temperatureValueTextview.setText(R.string.initial_temperature_value);
        humidityValueTextview.setText(R.string.initial_humidity_value);
        airPressureValueTextview.setText(R.string.initial_air_pressure_value);
    }

    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (connectedDeviceInfo == null) {
                        connectedDeviceInfo = new BleDeviceInfo(device, rssi);
                        stopScanningForBluetoothLeDevices();
                        connectToDevice(connectedDeviceInfo);
                    }
                }
            });
        }
    };

    private void connectToDevice(BleDeviceInfo device) {
        // Register the BroadcastReceiver to handle events from BluetoothAdapter and BluetoothLeService
        IntentFilter bluetoothEventFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        bluetoothEventFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        
        bluetoothEventReceiver = new BlueToothEventReceiver();
        registerReceiver(bluetoothEventReceiver, bluetoothEventFilter);

        startBluetoothLeService();
    }

    private void startBluetoothLeService() {
        bindIntent = new Intent(this, BluetoothLeService.class);
        startService(bindIntent);
        boolean serviceSucessfullyBound = bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (serviceSucessfullyBound)
            Log.d(TAG, "BluetoothLeService was sucessfully bound");
        else {
            Toast.makeText(this, "Bind to BluetoothLeService failed", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
