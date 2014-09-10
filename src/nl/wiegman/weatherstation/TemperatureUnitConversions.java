package nl.wiegman.weatherstation;

public final class TemperatureUnitConversions {

	private TemperatureUnitConversions() {
		// Utility class does not need a constructor
	}
	
    public static double convertCelciusToFahrenheit(double degreeCelcius) {
        return 32 + (degreeCelcius * 9 / 5);
    }
	
    public static double convertFahrenheitToCelcius(double degreeFahrenheit) {
    	return (degreeFahrenheit - 32) * 5 / 9;
    }
}
