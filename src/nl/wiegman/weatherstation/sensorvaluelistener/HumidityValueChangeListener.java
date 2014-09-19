package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface HumidityValueChangeListener {

	void humidityChanged(Context context, Double updatedHumidity);
}
