package nl.wiegman.weatherstation.service.history.impl;

/**
 * Represents an item from the history database 
 */
public class SensorValueHistoryItem {

	private int id;
	private long timestamp;
	private String sensorName;
	private double sensorValue;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public double getSensorValue() {
		return sensorValue;
	}

	public void setSensorValue(double sensorValue) {
		this.sensorValue = sensorValue;
	}

	@Override
	public String toString() {
		return "SensorHistoryItem [id=" + id + ", timestamp=" + timestamp + ", sensorName=" + sensorName + ", sensorValue=" + sensorValue + "]";
	}
}
