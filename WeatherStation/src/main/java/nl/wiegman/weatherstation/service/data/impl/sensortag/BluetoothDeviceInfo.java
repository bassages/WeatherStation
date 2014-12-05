package nl.wiegman.weatherstation.service.data.impl.sensortag;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceInfo {
    private BluetoothDevice bluetoothDevice;

    public BluetoothDeviceInfo(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
}
