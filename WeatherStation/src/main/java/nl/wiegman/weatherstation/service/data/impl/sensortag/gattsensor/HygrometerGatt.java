package nl.wiegman.weatherstation.service.data.impl.sensortag.gattsensor;

import java.util.UUID;

import static java.util.UUID.fromString;

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
 * To obtain data either use notifications or read the data directly. The update rate ranges from 100 ms to 2.55 seconds.
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
    public SensorData convert(byte[] byteValue) {
        int rawHumidity = shortUnsignedAtOffset(byteValue, 2);
        int temperatureRaw = shortUnsignedAtOffset(byteValue, 0);
        return convert(rawHumidity, temperatureRaw);
    }

    @Override
    public SensorData convert(String hexValue) {
        String[] hexValues = hexValue.split(" ");
        int rawTemperature = Integer.parseInt(hexValues[1] + hexValues[0], 16);
        int rawHumidity = Integer.parseInt(hexValues[3] + hexValues[2], 16);
        return convert(rawHumidity, rawTemperature);
    }

    private SensorData convert(Integer rawHumidity, Integer temperatureRaw) {
        float humidity = getHumidity(rawHumidity);
        float temperature = getAmbientTemperature(temperatureRaw);

//        Log.i(this.getClass().getSimpleName(), "Ambient temperature from hygrometer sensor: " + temperature);

        return new SensorData(humidity, temperature, 0);
    }

    private float getAmbientTemperature(int temperatureRaw) {
        return -46.85f + 175.72f/65536f *(float)temperatureRaw;
    }

    private float getHumidity(int rawHumidity) {
        // bits [1..0] are status bits and need to be cleared according to the user guide,
        // but the iOS code doesn't bother. It should have minimal impact.
        rawHumidity = rawHumidity - (rawHumidity % 4);
        return (-6f) + 125f * (rawHumidity / 65535f);
    }

    @Override
    public void calibrate() {
        // Not needed for this sensor
    }
}
