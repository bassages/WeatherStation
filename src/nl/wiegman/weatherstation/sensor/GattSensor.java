package nl.wiegman.weatherstation.sensor;

import java.util.UUID;

public interface GattSensor {

    SensorData convert(byte[] value);

    /**
     * @return the code which, when written to the configuration characteristic,
     *         turns on the sensor.
     * */
    byte getEnableSensorCode();

    UUID getService();

    abstract UUID getData();

    abstract UUID getConfig();

    byte getDisableSensorCode();

    void calibrate();
}