package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface AmbientTemperatureListener {

	// Gets called when a periodic update of the ambient temperature took place
	void ambientTemperatureUpdate(Context context, Double updatedTemperature);
}
