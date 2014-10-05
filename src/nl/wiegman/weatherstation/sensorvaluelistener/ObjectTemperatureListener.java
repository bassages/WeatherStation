package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface ObjectTemperatureListener {

	void objectTemperatureUpdate(Context context, Double updatedTemperature);
}
