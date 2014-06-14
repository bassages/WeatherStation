package nl.wiegman.weatherstation.sensor;

import java.util.UUID;

import android.util.Log;
import static java.util.UUID.*;

public class ThermometerGatt extends AbstractGattSensor {

    private static final UUID UUID_SERVICE = fromString("f000aa00-0451-4000-b000-000000000000");
    private static final UUID UUID_DATA = fromString("f000aa01-0451-4000-b000-000000000000");
    private static final UUID UUID_CONFIGURATION = fromString("f000aa02-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    private static final UUID UUID_UPDATE_RATE = fromString("f000aa03-0451-4000-b000-000000000000"); // Period in tens of milliseconds
    
    public ThermometerGatt() {
        super(UUID_SERVICE, UUID_DATA, UUID_CONFIGURATION);
    }
    
    @Override
    public SensorData convert(final byte[] value) {
        /*
         * The IR Temperature sensor produces two measurements; Object ( AKA
         * target or IR) Temperature, and Ambient ( AKA die ) temperature.
         * 
         * Both need some conversion, and Object temperature is dependent on
         * Ambient temperature.
         * 
         * They are stored as [ObjLSB, ObjMSB, AmbLSB, AmbMSB] (4 bytes)
         * Which means we need to shift the bytes around to get the correct
         * values.
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
