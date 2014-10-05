package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface AmbientTemperatureListener {

	void ambientTemperatureUpdate(Context context, Double updatedTemperature);
}
