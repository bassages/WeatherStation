package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface BarometricPressureListener {

	void barometricPressureUpdate(Context context, Double updatedBarometricPressure);
}
