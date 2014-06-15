package nl.wiegman.weatherstation.gattsensor;

import java.util.UUID;

public interface GattSensor {

    SensorData convert(byte[] value);

    /**
     * @return the code which, when written to the configuration characteristic,
     *         turns on the sensor.
     * */
    byte getEnableSensorCode();

    /**
     * @return the code which, when written to the configuration characteristic,
     *         turns off the sensor.
     * */
    byte getDisableSensorCode();

    UUID getService();

    UUID getData();

    UUID getConfig();

    void calibrate();
}