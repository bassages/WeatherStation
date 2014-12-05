package nl.wiegman.weatherstation.sensorvaluelistener;

import nl.wiegman.weatherstation.SensorType;
import android.content.Context;

public interface SensorValueListener {

	// Gets called when a periodic update of the sensor value took place
	void valueUpdate(Context context, SensorType sensorType, Double updatedValue);
}
