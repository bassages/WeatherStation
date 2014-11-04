package nl.wiegman.weatherstation.history;

import nl.wiegman.weatherstation.sensorvaluelistener.AmbientTemperatureListener;
import android.content.Context;

/**
 * Maintains the ambient temperature history
 */
public class AmbientTemperatureHistory extends AbstractSensorHistory implements AmbientTemperatureListener {

	private static final String SENSOR_NAME = "ambient_temperature";
	
	@Override
	public void ambientTemperatureUpdate(final Context context, final Double updatedTemperature) {
		registerUpdatedValue(context, updatedTemperature);
	}

	@Override
	protected String getSensorName() {
		return SENSOR_NAME;
	}
}
