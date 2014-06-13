package nl.wiegman.weatherstation;

import static nl.wiegman.weatherstation.SensorTag.UUID_BAR_DATA;
import static nl.wiegman.weatherstation.SensorTag.UUID_HUM_DATA;
import static nl.wiegman.weatherstation.SensorTag.UUID_IRT_DATA;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.wiegman.weatherstation.sensor.BarometerCalibrationCoefficients;
import nl.wiegman.weatherstation.sensor.Sensor;
import nl.wiegman.weatherstation.sensor.SensorData;
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
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
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
    // Log
    private static final String TAG = "MainActivity";

    // Requests to other activities
    private static final int REQUEST_TO_ENABLE_BLUETOOTHE_LE = 0;

    private volatile BleDeviceInfo connectedDeviceInfo;

    private static final int GATT_TIMEOUT = 300; // milliseconds
    private static final double PA_PER_METER = 12.0;

    private Context thisContext = this;

    private Intent bindIntent;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;
    
    private BroadcastReceiver sensortagUpdateReceiver;
    private BroadcastReceiver bluetoothEventReceiver;
    
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

        // GATT database for the TI sensortag
        Resources res = getResources();
        XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
        new GattInfo(xpp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        
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
                clearSensorValues();
                
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

    private void clearSensorValues() {
        TextView temperatureTextView = (TextView) findViewById(R.id.temperatureValue);
        temperatureTextView.setText(R.string.initial_temperature_value);
 
        TextView humidityValueTextView = (TextView) findViewById(R.id.humidityValue);
        humidityValueTextView.setText(R.string.initial_humidity_value);
        
        TextView airPressureValueTextView = (TextView) findViewById(R.id.airPressureValue);
        airPressureValueTextView.setText(R.string.initial_air_pressure_value);
    }
    
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
                return;
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

    private void enableSensor(Sensor sensor, boolean enable) {
        UUID servUuid = sensor.getService();
        UUID confUuid = sensor.getConfig();

        // Barometer calibration
        if (confUuid.equals(SensorTag.UUID_BAR_CONF) && enable) {
            calibrateBarometer();
        }

        BluetoothGattService serv = BluetoothLeService.getBtGatt().getService(servUuid);
        BluetoothGattCharacteristic charac = serv.getCharacteristic(confUuid);
        byte value = enable ? sensor.getEnableSensorCode() : Sensor.DISABLE_SENSOR_CODE;
        
        boolean characteristicWritten = false;
        if (!characteristicWritten) {
            while (!characteristicWritten) {
                characteristicWritten = BluetoothLeService.getInstance().writeCharacteristic(charac, value);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        BluetoothLeService.getInstance().waitIdle(GATT_TIMEOUT);
    }
    
    /* Calibrating the barometer includes
     * 
     * 1. Write calibration code to configuration characteristic. 
     * 2. Read calibration values from sensor, either with notifications or a normal read. 
     * 3. Use calibration values in formulas when interpreting sensor values.
     */
    private void calibrateBarometer() {
        Log.i(TAG, "calibrateBarometer");
        
        UUID servUuid = Sensor.BAROMETER.getService();
        UUID configUuid = Sensor.BAROMETER.getConfig();
        BluetoothGattService serv = BluetoothLeService.getBtGatt().getService(servUuid);
        BluetoothGattCharacteristic config = serv.getCharacteristic(configUuid);

        // Write the calibration code to the configuration registers
        BluetoothLeService.getInstance().writeCharacteristic(config,Sensor.CALIBRATE_SENSOR_CODE);
        BluetoothLeService.getInstance().waitIdle(GATT_TIMEOUT);
        BluetoothGattCharacteristic calibrationCharacteristic = serv.getCharacteristic(SensorTag.UUID_BAR_CALI);
        BluetoothLeService.getInstance().readCharacteristic(calibrationCharacteristic);
        BluetoothLeService.getInstance().waitIdle(GATT_TIMEOUT);
    }
    
    private void enableNotifications(Sensor sensor, boolean enable) {
        UUID servUuid = sensor.getService();
        UUID dataUuid = sensor.getData();
        BluetoothGattService serv = BluetoothLeService.getBtGatt().getService(servUuid);
        BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

        BluetoothLeService.getInstance().setCharacteristicNotification(charac, enable);
        BluetoothLeService.getInstance().waitIdle(GATT_TIMEOUT);
    }

    private void discoverServices() {
        if (BluetoothLeService.getBtGatt().discoverServices()) {
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
                    enableSensor(Sensor.HUMIDITY, true);
                    enableNotifications(Sensor.HUMIDITY, true);

                    enableSensor(Sensor.BAROMETER, true);
                    enableNotifications(Sensor.BAROMETER, true);

                    enableSensor(Sensor.TEMPERATURE, true);
                    enableNotifications(Sensor.TEMPERATURE, true);
                } else {
                    Toast.makeText(getApplication(), "Service discovery failed", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                onCharacteristicChanged(uuidStr, value);
            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                // Data written
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                onCharacteristicWrite(uuidStr, status);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                // Data read
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicsRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT error code: " + status);
            }
        }
    };

    private void onCharacteristicWrite(String uuidStr, int status) {
        Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
    }

    private void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
        if (uuidStr.equals(SensorTag.UUID_BAR_CALI.toString())) {
            Log.i(TAG, "The barometer was sucessfully calibrated");
            // Barometer calibration values are read.
            List<Integer> cal = new ArrayList<Integer>();
            for (int offset = 0; offset < 8; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1] & 0xFF;
                cal.add((upperByte << 8) + lowerByte);
            }

            for (int offset = 8; offset < 16; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1];
                cal.add((upperByte << 8) + lowerByte);
            }

            BarometerCalibrationCoefficients.INSTANCE.barometerCalibrationCoefficients = cal;
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
     * */
    public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
        SensorData v;
        String msg;
        
        TextView temperatureTextview = (TextView) findViewById(R.id.temperatureValue);
        if (uuidStr.equals(UUID_IRT_DATA.toString())) {
            v = Sensor.TEMPERATURE.convert(rawValue);
            msg = new DecimalFormat("0.0").format(v.x);
            temperatureTextview.setText(msg);
        }
        TextView mHumValue = (TextView) findViewById(R.id.humidityValue);
        if (uuidStr.equals(UUID_HUM_DATA.toString())) {
            v = Sensor.HUMIDITY.convert(rawValue);
            msg = new DecimalFormat("0.0").format(v.x);
            mHumValue.setText(msg);
        }
        TextView mBarValue = (TextView) findViewById(R.id.airPressureValue);
        if (uuidStr.equals(UUID_BAR_DATA.toString())) {
            v = Sensor.BAROMETER.convert(rawValue);
            double h = (v.x - BarometerCalibrationCoefficients.INSTANCE.heightCalibration) / PA_PER_METER;
            h = (double)Math.round(-h * 10.0) / 10.0;
            msg = new DecimalFormat("0").format(v.x/100);
            mBarValue.setText(msg);
        }
    }

    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (connectedDeviceInfo == null) {
                        connectedDeviceInfo = createDeviceInfo(device, rssi);
                        stopScanningForBluetoothLeDevices();
                        connectToDevice(connectedDeviceInfo);
                    }
                }
            });
        }
    };

    private void connectToDevice(BleDeviceInfo device) {
        // Register the BroadcastReceiver to handle events from BluetoothAdapter
        // and BluetoothLeService
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

    private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
        return new BleDeviceInfo(device, rssi);
    }
}
