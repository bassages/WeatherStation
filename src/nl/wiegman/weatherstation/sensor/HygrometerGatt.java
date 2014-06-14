package nl.wiegman.weatherstation.sensor;

import java.util.UUID;

import static java.util.UUID.*;

public class HygrometerGatt extends AbstractGattSensor {

    private static final UUID UUID_HUM_SERV = fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID UUID_HUM_DATA = fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID UUID_HUM_CONF = fromString("f000aa22-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    private static final UUID UUID_HUM_PERI = fromString("f000aa23-0451-4000-b000-000000000000"); // Period in tens of milliseconds
    
    public HygrometerGatt() {
        super(UUID_HUM_SERV, UUID_HUM_DATA, UUID_HUM_CONF);
    }

    @Override
    public SensorData convert(final byte[] value) {
        int a = shortUnsignedAtOffset(value, 2);
        // bits [1..0] are status bits and need to be cleared according
        // to the user guide, but the iOS code doesn't bother. It should
        // have minimal impact.
        a = a - (a % 4);

        return new SensorData((-6f) + 125f * (a / 65535f), 0, 0);
    }

    @Override
    public void calibrate() {
        // Not needed for this sensor
    }
}
