package nl.wiegman.weatherstation.gattsensor;

import java.util.UUID;

public abstract class AbstractGattSensor implements GattSensor {

    private final UUID service, data, config;
    
    public static final byte DISABLE_SENSOR_CODE = 0;
    public static final byte ENABLE_SENSOR_CODE = 1;

    @Override
    public abstract SensorData convert(byte[] value);
    
    /**
     * Constructor
     * */
    protected AbstractGattSensor(UUID service, UUID data, UUID config) {
        this.service = service;
        this.data = data;
        this.config = config;
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
    public byte getEnableSensorCode() {
        return ENABLE_SENSOR_CODE;
    }
    
    @Override
    public byte getDisableSensorCode() {
        return DISABLE_SENSOR_CODE;
    }

    @Override
    public UUID getService() {
        return service;
    }

    @Override
    public UUID getData() {
        return data;
    }

    @Override
    public UUID getConfig() {
        return config;
    }
}
