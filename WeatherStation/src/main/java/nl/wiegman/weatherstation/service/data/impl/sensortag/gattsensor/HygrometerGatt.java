package nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor;

import static java.util.UUID.fromString;

import java.util.UUID;

/**
 * Hardware on TI SensorTag: Sensirion SHT21 @ U6
 *
 * Two types of data are obtained from the Humidity sensor, relative humidity and ambient temperature
 * 
 * ----------------------------------------------------------------------------------------
 * | Type                | UUID   | Read/Write  | Format                                  |
 * |--------------------------------------------------------------------------------------|
 * | <Data>              | AA21 * | Read/Notify | TempLSB TempMSB HumLSB HumMSB (4 bytes) |
 * | <Data Notification> | -      | R/W         | 2 bytes                                 |
 * | <Configuration>     | AA22 * | R/W         | 1 byte                                  |
 * | <Period>            | AA23 * | R/W         | 1 byte                                  |
 * ----------------------------------------------------------------------------------------
 *
 * The driver for this sensor is using a state machine so when the enable command is issued, the sensor starts to perform one measurements and the data is stored in the <Data>. 
 * 
 * To obtain data OTA either use notifications or read the data directly. The update rate ranges from 100 ms to 2.55 seconds.
 * 
 * The humidity and temperature data in the sensor is issued and measured explicitly where the humidity data takes ~64ms to measure. 
 */
public class HygrometerGatt extends AbstractGattSensor {

    private static final UUID UUID_SERVICE = fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID UUID_DATA = fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID UUID_CONFIGURATION = fromString("f000aa22-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    
    public HygrometerGatt() {
        super(UUID_SERVICE, UUID_DATA, UUID_CONFIGURATION);
    }

    @Override
    public SensorData convert(final byte[] value) {
        float humidity = getHumidity(value);
        float temperature = getAmbientTemperature(value);
        
//        Log.i(this.getClass().getSimpleName(), "Ambient temperature from hygrometer sensor: " + temperature);
        
        return new SensorData(humidity, temperature, 0);
    }

    private float getAmbientTemperature(final byte[] value) {
        int temperatureRaw = shortUnsignedAtOffset(value, 0);
        return -46.85f + 175.72f/65536f *(float)temperatureRaw;
    }

    private float getHumidity(final byte[] value) {
        int a = shortUnsignedAtOffset(value, 2);
        // bits [1..0] are status bits and need to be cleared according
        // to the user guide, but the iOS code doesn't bother. It should
        // have minimal impact.
        a = a - (a % 4);
        return (-6f) + 125f * (a / 65535f);
    }

    @Override
    public void calibrate() {
        // Not needed for this sensor
    }
}
