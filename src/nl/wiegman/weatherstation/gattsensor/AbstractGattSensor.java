package nl.wiegman.weatherstation.gattsensor;

import java.util.UUID;

import nl.wiegman.weatherstation.bluetooth.BluetoothLeService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public abstract class AbstractGattSensor implements GattSensor {

    private final UUID serviceUuid, dataUuid, configUuid;
    
    public static final byte DISABLE_SENSOR_CODE = 0;
    public static final byte ENABLE_SENSOR_CODE = 1;

    protected String LOG_TAG = this.getClass().getSimpleName();
    
    @Override
    public abstract SensorData convert(byte[] value);
    
    /**
     * Constructor
     * */
    protected AbstractGattSensor(UUID serviceUuid, UUID dataUuid, UUID configUuid) {
        this.serviceUuid = serviceUuid;
        this.dataUuid = dataUuid;
        this.configUuid = configUuid;
    }
    
    /**
     * Barometer, IR temperature all store 16 bit two's complement values in the
     * awkward format LSB MSB, which cannot be directly parsed as
     * getIntValue(FORMAT_SINT16, offset) because the bytes are stored in the
     * "wrong" direction.
     * 
     * This function extracts these 16 bit two's complement values.
     * */
    protected Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1]; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    protected Integer shortUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1] & 0xFF; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }
    
    @Override
    public void enable() {
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(configUuid);
        
        byte value = getEnableSensorCode();
        BluetoothLeService.getInstance().initiateWriteCharacteristic(characteristic, value);
    }
    
    @Override
    public void disable() {
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(configUuid);
        
        byte value = getDisableSensorCode();
        BluetoothLeService.getInstance().initiateWriteCharacteristic(characteristic, value);
    }
    
    @Override
    public void enableNotifications() {
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(dataUuid);

        boolean setNotificationCharacteristicSuccesfull = BluetoothLeService.getInstance().initiateNotificationCharacteristic(characteristic, true);
        if (!setNotificationCharacteristicSuccesfull) {
            Log.e(LOG_TAG, "Failed to setNotificationCharacteristic");
        }
    }
    
    @Override
    public void disableNotifications() {
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(dataUuid);

        boolean setNotificationCharacteristicSuccesfull = BluetoothLeService.getInstance().initiateNotificationCharacteristic(characteristic, false);
        if (!setNotificationCharacteristicSuccesfull) {
            Log.e(LOG_TAG, "Failed to setNotificationCharacteristic");
        }
    }
    
    @Override
    public byte getEnableSensorCode() {
        return ENABLE_SENSOR_CODE;
    }
    
    @Override
    public byte getDisableSensorCode() {
        return DISABLE_SENSOR_CODE;
    }

    @Override
    public UUID getServiceUuid() {
        return serviceUuid;
    }

    @Override
    public UUID getDataUuid() {
        return dataUuid;
    }

    @Override
    public UUID getConfigUuid() {
        return configUuid;
    }
}
