package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface TemperatureValueChangeListener {

	void temperatureChanged(Context context, Double updatedTemperature);
}
