package nl.wiegman.weatherstation;

public class SensorState {

    private double temperature = 0.0;
    private int humidity = 50;
    private int airPressure = 0;
    
	public double getTemperature() {
		return temperature;
	}
	
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	
	public int getHumidity() {
		return humidity;
	}
	
	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}
	
	public int getAirPressure() {
		return airPressure;
	}
	
	public void setAirPressure(int airPressure) {
		this.airPressure = airPressure;
	}
}
