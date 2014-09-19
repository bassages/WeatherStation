package nl.wiegman.weatherstation.sensorvaluelistener;

import android.content.Context;

public interface BarometricPressureValueChangeListener {

	void barometricPressureChanged(Context context, Double updatedBarometricPressure);
}
