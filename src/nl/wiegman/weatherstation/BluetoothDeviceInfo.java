package nl.wiegman.weatherstation;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceInfo {
    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public BluetoothDeviceInfo(BluetoothDevice bluetoothDevice, int rssi) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public void updateRssi(int rssiValue) {
        rssi = rssiValue;
    }
}
