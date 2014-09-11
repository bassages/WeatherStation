package nl.wiegman.weatherstation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import nl.wiegman.weatherstation.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class TemperatureUtil {

	private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.0;-0.0");
	
	private TemperatureUtil() {
		// Utility class does not need a constructor
	}
    
    public static double convertFromPreferenceUnitToStorageUnit(Context context, double temperatureValueInPreferenceUnit) {
    	double result = temperatureValueInPreferenceUnit;
    	
    	String fahrenheit = context.getString(R.string.temperature_unit_degree_fahrenheit);    	
    	String preferredTemperatureUnit = getPreferredTemperatureUnit(context);
    	
    	if (fahrenheit.equals(preferredTemperatureUnit)) {
    		result = convertFahrenheitToCelcius(temperatureValueInPreferenceUnit);
    	}
		return round(result);
	}

	public static double convertFromStorageUnitToPreferenceUnit(Context context, double temperatureValueInStorageUnit) {
    	double result = temperatureValueInStorageUnit;
    	
    	String fahrenheit = context.getString(R.string.temperature_unit_degree_fahrenheit);    	
    	String preferredTemperatureUnit = getPreferredTemperatureUnit(context);
    	
    	if (fahrenheit.equals(preferredTemperatureUnit)) {
    		result = convertCelciusToFahrenheit(temperatureValueInStorageUnit);
    	}
		return round(result);
	}
    
    public static String getPreferredTemperatureUnit(Context context) {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    	
        String temperatureUnitPreferenceKey = context.getString(R.string.preference_temperature_unit_key);
        String temperatureUnitDefaultValue = context.getString(R.string.preference_temperature_unit_default_value);
        return preferences.getString(temperatureUnitPreferenceKey, temperatureUnitDefaultValue);
    }

    public static double round(double value) {
		return new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}
    
    public static String format(Double temperature) {
    	return VALUE_FORMAT.format(temperature);
    }
    
    private static double convertCelciusToFahrenheit(double degreeCelcius) {
        return 32 + (degreeCelcius * 9 / 5);
    }
	
    private static double convertFahrenheitToCelcius(double degreeFahrenheit) {
    	return (degreeFahrenheit - 32) * 5 / 9;
    }
}
