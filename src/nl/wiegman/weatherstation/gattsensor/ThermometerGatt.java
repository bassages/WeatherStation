package nl.wiegman.weatherstation.gattsensor;

import java.util.UUID;

import android.util.Log;
import static java.util.UUID.*;

/**
 * <pre>
 *
 * Texas Instruments TMP006 @ U5 Two types of data are obtained from the IR Temperature sensor: object temperature and ambient temperature.
 * 
 * --------------------------------------------------------------------------------------
 * | Type                |  UUID  |  Read/Write | Format                                |
 * |------------------------------------------------------------------------------------|
 * | <Data>              | AA01 * | Read/Notify | ObjLSB ObjMSB AmbLSB AmbMSB (4 bytes) |
 * | <Data Notification> | -      | R/W         | 2 bytes                               |
 * | <Configuration>     | AA02 * | R/W         | 1 byte                                |
 * | <Period>            | AA03 * | R/W         | 1 byte                                |
 * --------------------------------------------------------------------------------------
 *
 * When the enable command is issued, the sensor starts to perform measurements each second (average over four measurements) 
 * and the data is stored in the <Data> each second as well. When the disable command is issued, the sensor is put in stand-by mode. 
 * 
 * To obtain data OTA either use notifications or read the data directly. 
 * The period range varies from 300 ms to 2.55 seconds. The unit is 10 ms. i.e. writing 0x32 gives 500 ms, 0x64 1 second etc. The default value is 1 second.
 *
 * For more information please refer to TI TMP006 User's Guide
 *
 * The raw data value read from this sensor are two unsigned 16 bit values, one for die temperature and one for object temperature.
 * 
 * </pre>
 */
public class ThermometerGatt extends AbstractGattSensor {
    private static final UUID UUID_SERVICE = fromString("f000aa00-0451-4000-b000-000000000000");
    private static final UUID UUID_DATA = fromString("f000aa01-0451-4000-b000-000000000000");
    private static final UUID UUID_CONFIGURATION = fromString("f000aa02-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    
    public ThermometerGatt() {
        super(UUID_SERVICE, UUID_DATA, UUID_CONFIGURATION);
    }
    
    @Override
    public SensorData convert(final byte[] value) {
        /*
         * The IR Temperature sensor produces two measurements; Object (AKA target or IR) Temperature, and Ambient ( AKA die ) temperature.
         * 
         * Both need some conversion, and Object temperature is dependent on Ambient temperature.
         * 
         * They are stored as [ObjLSB, ObjMSB, AmbLSB, AmbMSB] (4 bytes)
         * Which means we need to shift the bytes around to get the correct values.
         */
        double ambient = extractAmbientTemperature(value);
        
        Log.i(this.getClass().getSimpleName(), "Ambient temperature from thermometer sensor: " + ambient);
        
        return new SensorData(ambient, 0, 0);
    }

    private double extractAmbientTemperature(byte[] v) {
        int offset = 2;
        return shortUnsignedAtOffset(v, offset) / 128.0;
    }
    
    @Override
    public void calibrate() {
        // Not needed for this sensor
    }
}
