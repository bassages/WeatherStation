package nl.wiegman.weatherstation.history;

import nl.wiegman.weatherstation.sensorvaluelistener.ObjectTemperatureListener;
import android.content.Context;

/**
 * Maintains the object temperature history
 */
public class ObjectTemperatureHistory extends AbstractSensorHistory implements ObjectTemperatureListener {

	private static final String SENSOR_NAME = "object_temperature";
	
	@Override
	public void objectTemperatureUpdate(final Context context, final Double updatedTemperature) {
		registerUpdatedValue(context, updatedTemperature);
	}

	@Override
	protected String getSensorName() {
		return SENSOR_NAME;
	}
}
