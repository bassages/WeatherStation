package nl.wiegman.weatherstation.util;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import nl.wiegman.weatherstation.R;

public final class TemperatureUtil {

	private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.0;-0.0");
	
	private TemperatureUtil() {
		// Utility class does not need a constructor
	}
    
    public static double convertFromPreferenceUnitToStorageUnit(Context context, double temperatureValueInPreferenceUnit) {
    	double result = 0;
    	
    	String fahrenheit = context.getString(R.string.temperature_unit_degree_fahrenheit);
    	
    	String preferredTemperatureUnit = getPreferredTemperatureUnit(context);
    	if (fahrenheit.equals(preferredTemperatureUnit)) {
    		result = convertFahrenheitToCelcius(temperatureValueInPreferenceUnit);
    	} else {
    		result = temperatureValueInPreferenceUnit;
    	}
		return result;
	}

    public static double convertFromStorageUnitToPreferenceUnit(Context context, double temperatureValueInStorageUnit) {
    	double result = 0;
    	
    	String fahrenheit = context.getString(R.string.temperature_unit_degree_fahrenheit);
    	
    	String preferredTemperatureUnit = getPreferredTemperatureUnit(context);
    	if (fahrenheit.equals(preferredTemperatureUnit)) {
    		result = convertCelciusToFahrenheit(temperatureValueInStorageUnit);
    	} else {
    		result = temperatureValueInStorageUnit;
    	}
		return result;
	}
    
    public static String getPreferredTemperatureUnit(Context context) {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    	
        String temperatureUnitPreferenceKey = context.getString(R.string.preference_temperature_unit_key);
        String temperatureUnitDefaultValue = context.getString(R.string.preference_temperature_unit_default_value);
        return preferences.getString(temperatureUnitPreferenceKey, temperatureUnitDefaultValue);
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
