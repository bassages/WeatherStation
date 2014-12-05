package nl.wiegman.weatherstation.service.history.impl;

import nl.wiegman.weatherstation.SensorType;

/**
 * Holds one value of one sensor on one moment in time 
 */
public class SensorValueHistoryItem {

	private int id;
	private long timestamp;
	private SensorType sensorType;
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

	public double getSensorValue() {
		return sensorValue;
	}

	public void setSensorValue(double sensorValue) {
		this.sensorValue = sensorValue;
	}

	public SensorType getSensorType() {
		return sensorType;
	}

	public void setSensorType(SensorType sensorType) {
		this.sensorType = sensorType;
	}

	@Override
	public String toString() {
		return "SensorHistoryItem [id=" + id + ", timestamp=" + timestamp + ", sensorType=" + sensorType + ", sensorValue=" + sensorValue + "]";
	}
}
