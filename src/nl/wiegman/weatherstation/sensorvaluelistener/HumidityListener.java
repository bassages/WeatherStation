package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface HumidityListener {

	void humidityUpdate(Context context, Double updatedHumidity);
}
