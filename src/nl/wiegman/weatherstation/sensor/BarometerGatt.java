package nl.wiegman.weatherstation.sensor;

import static java.lang.Math.pow;
import static java.util.UUID.fromString;

import java.util.List;
import java.util.UUID;

import nl.wiegman.weatherstation.BluetoothLeService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public class BarometerGatt extends AbstractGattSensor {

    private static final UUID UUID_SERVICE = fromString("f000aa40-0451-4000-b000-000000000000");
    private static final UUID UUID_DATA = fromString("f000aa41-0451-4000-b000-000000000000");
    public static final UUID UUID_CONFIGURATION = fromString("f000aa42-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    public static final UUID UUID_CALIBRATION = fromString("f000aa43-0451-4000-b000-000000000000"); // Calibration characteristic
    private static final UUID UUID_NOTIFICATION_RATE = fromString("f000aa44-0451-4000-b000-000000000000"); // Period in tens of milliseconds
    
    public BarometerGatt() {
        super(UUID_SERVICE, UUID_DATA, UUID_CONFIGURATION);
    }

    public static final double PA_PER_METER = 12.0;
    
    @Override
    public SensorData convert(final byte[] value) {

        List<Integer> barometerCalibrationCoefficients = BarometerCalibrationCoefficients.INSTANCE.barometerCalibrationCoefficients;
        if (barometerCalibrationCoefficients == null) {
            Log.w("Custom", "Data notification arrived for barometer before it was calibrated.");
            return new SensorData(0, 0, 0);
        }

        final int[] c; // Calibration coefficients
        final Integer t_r; // Temperature raw value from sensor
        final Double t_a;   // Temperature actual value in unit centi degrees celsius
        final Integer p_r; // Pressure raw value from sensor
        final Double S; // Interim value in calculation
        final Double O; // Interim value in calculation
        final Double p_a; // Pressure actual value in unit Pascal.

        c = new int[barometerCalibrationCoefficients.size()];
        for (int i = 0; i < barometerCalibrationCoefficients.size(); i++) {
            c[i] = barometerCalibrationCoefficients.get(i);
        }

        t_r = shortSignedAtOffset(value, 0);
        p_r = shortUnsignedAtOffset(value, 2);

        t_a = ((100 * (c[0] * t_r / pow(2,8) + c[1] * pow(2,6))) / pow(2,16)) / 100;
        S = c[2] + c[3] * t_r / pow(2,17) + ((c[4] * t_r / pow(2,15)) * t_r) / pow(2,19);
        O = c[5] * pow(2,14) + c[6] * t_r / pow(2,3) + ((c[7] * t_r / pow(2,15)) * t_r) / pow(2,4);
        p_a = ((S * p_r + O) / pow(2,14)) / 100;

        Log.i("BarometerGatt", "Ambient temperature from barometer sensor: " + t_a);
        
        return new SensorData(p_a, t_a, 0);
    }
    
    /* Calibrating the barometer includes
     * 
     * 1. Write calibration code to configuration characteristic. 
     * 2. Read calibration values from sensor, either with notifications or a normal read. 
     * 3. Use calibration values in formulas when interpreting sensor values.
     */
    public void calibrate() {
        BluetoothGattService service = BluetoothLeService.getBtGatt().getService(getService());
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(getConfig());
        
        BluetoothLeService.getInstance().writeCharacteristic(characteristic, BarometerGatt.CALIBRATE_SENSOR_CODE);
        
        BluetoothGattCharacteristic calibrationCharacteristic = service.getCharacteristic(BarometerGatt.UUID_CALIBRATION);
        BluetoothLeService.getInstance().readCharacteristic(calibrationCharacteristic);
    }
}
