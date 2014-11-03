package nl.wiegman.weatherstation.fragment;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nl.wiegman.weatherstation.MainActivity;
import nl.wiegman.weatherstation.history.AmbientTemperatureHistory;
import nl.wiegman.weatherstation.history.SensorValueHistoryItem;
import nl.wiegman.weatherstation.sensorvaluelistener.AmbientTemperatureListener;

/**
 * <pre>
 * Fragment containing a graph that shows the sensor values over time.
 * "Domain" value is timestamp at which the sample has been taken
 * "Range" value is the sensor value
 * </pre>
 */
public class AmbientTemperatureHistoryFragment extends TemperatureHistoryFragment implements AmbientTemperatureListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
        // Register as a temperature listener
        ((MainActivity)getActivity()).addAmbientTemperatureListener(this);
        return view;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		((MainActivity)getActivity()).removeAmbientTemperatureListener(this);
	}
	
	@Override
	public void ambientTemperatureUpdate(Context context, Double updatedTemperature) {
		addToGraph(System.currentTimeMillis(), updatedTemperature);
	}
	
	@Override
	protected List<SensorValueHistoryItem> getHistoryItems() {
		AmbientTemperatureHistory historyStore = new AmbientTemperatureHistory();
		return historyStore.getAll(getActivity());
	}
}
