package nl.wiegman.weatherstation.gattsensor;

import static java.lang.Math.pow;
import static java.util.UUID.fromString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.wiegman.weatherstation.bluetooth.BluetoothLeService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

/**
 * <pre>

 * Epcos T5400-C953. Two types of data are obtained from the Barometric Pressure Sensor: pressure and ambient temperature
 * 
 * ---------------------------------------------------------------------------------------------|
 * | Type                |  UUID  | Read/Write | Format                                         |
 * |--------------------------------------------------------------------------------------------|
 * | <Data>              | AA41 * | Read only  | TempLSB TempMSB PressLSB PressMSB (4 bytes)    |
 * | <Data Notification> |  -     | R/W        | 2 bytes                                        |
 * | <Configuration>     | AA42 * | R/W        | 1 byte                                         |
 * | <Calibration>       | AA43 * | Read only  | C1LSB C1MSB ... C8LSB C8MSB (16 bytes)         |
 * | <Period>            | AA44 * | R/W        | 1 Byte                                         |
 * ----------------------------------------------------------------------------------------------
 * 
 * The driver for this sensor is using a state machine so when the enable command is issued, 
 * the sensor starts to perform one measurements and the data is stored in the <Data>. 
 * 
 * To obtain data OTA either use notifications or read the data directly. The period ranges for 100 ms to 2.55 seconds, resolution 10 ms.
 *
 * For calculation, the 8 16-bit pairs of calibration values are needed which is read from sensor and stored in the <Calibration> by writing 02 to <Configuration>. 
 * The Calibration values are obtained by either read from <Calibration> or if notification has been enabled automatically sent when available.
 * 
 * </pre> 
 */
public class BarometerGatt extends AbstractGattSensor {

    private static final UUID UUID_SERVICE = fromString("f000aa40-0451-4000-b000-000000000000");
    private static final UUID UUID_DATA = fromString("f000aa41-0451-4000-b000-000000000000");
    public static final UUID UUID_CONFIGURATION = fromString("f000aa42-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    public static final UUID UUID_CALIBRATION = fromString("f000aa43-0451-4000-b000-000000000000"); // Calibration characteristic
    
    public static final byte CALIBRATE_SENSOR_CODE = 2;
    
    private List<Integer> calibrationCoefficients = null;
    
    public BarometerGatt() {
        super(UUID_SERVICE, UUID_DATA, UUID_CONFIGURATION);
    }

    public static final double PA_PER_METER = 12.0;
    
    @Override
    public SensorData convert(final byte[] value) {
        SensorData sensorData = null;
        
        if (calibrationCoefficients == null) {
            Log.w("Custom", "Data notification arrived for barometer before it was calibrated.");
            sensorData = new SensorData(0, 0, 0);
        } else {
            final int[] c; // Calibration coefficients
            final Integer t_r; // Temperature raw value from sensor
            final Double t_a;   // Temperature actual value in unit centi degrees celsius
            final Integer p_r; // Pressure raw value from sensor
            final Double S; // Interim value in calculation
            final Double O; // Interim value in calculation
            final Double p_a; // Pressure actual value in unit Pascal.
            
            c = new int[calibrationCoefficients.size()];
            for (int i = 0; i < calibrationCoefficients.size(); i++) {
                c[i] = calibrationCoefficients.get(i);
            }
            
            t_r = shortSignedAtOffset(value, 0);
            p_r = shortUnsignedAtOffset(value, 2);
            
            t_a = ((100 * (c[0] * t_r / pow(2,8) + c[1] * pow(2,6))) / pow(2,16)) / 100;
            S = c[2] + c[3] * t_r / pow(2,17) + ((c[4] * t_r / pow(2,15)) * t_r) / pow(2,19);
            O = c[5] * pow(2,14) + c[6] * t_r / pow(2,3) + ((c[7] * t_r / pow(2,15)) * t_r) / pow(2,4);
            p_a = (S * p_r + O) / pow(2,14);
            
            double pressureInHpa = p_a / 100;
            
            sensorData = new SensorData(pressureInHpa, t_a, 0);
        }
        return sensorData;
    }
    
    /* Calibrating the barometer includes
     * 
     * 1. Write calibration code to configuration characteristic. 
     * 2. Read calibration values from sensor, either with notifications or a normal read. 
     * 3. Use calibration values in formulas when interpreting sensor values.
     */
    public void calibrate() {
        BluetoothGattService service = BluetoothLeService.getBluetoothGatt().getService(UUID_SERVICE);

        BluetoothGattCharacteristic configurationCharacteristic = service.getCharacteristic(UUID_CONFIGURATION);
        BluetoothLeService.getInstance().initiateWriteCharacteristic(configurationCharacteristic, CALIBRATE_SENSOR_CODE);
        
        BluetoothGattCharacteristic calibrationCharacteristic = service.getCharacteristic(BarometerGatt.UUID_CALIBRATION);
        BluetoothLeService.getInstance().initiateReadCharacteristic(calibrationCharacteristic);
    }

    public void processCalibrationResults(byte[] value) {
        Log.i(this.getClass().getSimpleName(), "The barometer was sucessfully calibrated");
        // Barometer calibration values are read.
        List<Integer> calibration = new ArrayList<Integer>();
        for (int offset = 0; offset < 8; offset += 2) {
            Integer lowerByte = (int) value[offset] & 0xFF;
            Integer upperByte = (int) value[offset + 1] & 0xFF;
            calibration.add((upperByte << 8) + lowerByte);
        }
        for (int offset = 8; offset < 16; offset += 2) {
            Integer lowerByte = (int) value[offset] & 0xFF;
            Integer upperByte = (int) value[offset + 1];
            calibration.add((upperByte << 8) + lowerByte);
        }
        this.calibrationCoefficients = calibration;
    }
}
