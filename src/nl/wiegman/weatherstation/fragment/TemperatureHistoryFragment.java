package nl.wiegman.weatherstation.fragment;

import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import android.content.Context;

public class TemperatureHistoryFragment extends SensorHistoryFragment implements TemperatureValueChangeListener {

	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
		super.valueChanged(updatedTemperature);
	}

	@Override
	protected String getTitle() {
		return "Temperature history";
	}
}
