package nl.wiegman.weatherstation.fragment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.history.SensorValueHistoryService;
import nl.wiegman.weatherstation.service.history.impl.SensorValueHistoryItem;
import nl.wiegman.weatherstation.service.history.impl.SensorValueHistoryServiceImpl;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import nl.wiegman.weatherstation.util.ThemeUtil;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.Plot.BorderStyle;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

/**
 * <pre>
 * Fragment containing a graph that shows the sensor values over time.
 * "Domain" value is timestamp at which the sample has been taken
 * "Range" value is the sensor value
 * </pre>
 */
public class TemperatureHistoryFragment extends Fragment implements SensorValueListener {
	private static final String LOG_TAG = SensorDataFragment.class.getSimpleName();
	
	private XYPlot plot;
	private SimpleXYSeries sensorValueHistorySeries;

	private Double maxAddedValue;
	private Double minAddedValue;
	
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
	private String preferenceTemperatureUnitKey;
    
	private Class<?> sensorDataProviderServiceClass;
	private SensorDataProviderService sensorDataProviderService;
	
	private SensorValueHistoryService sensorHistoryService;
	
	private SensorType sensorType;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
    	preferenceListener = new PreferenceListener();	
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		preferenceTemperatureUnitKey = getActivity().getApplicationContext().getString(R.string.preference_temperature_unit_key);
		
		Bundle arguments = getArguments();
		sensorType = SensorType.valueOf(arguments.getString(SensorType.class.getSimpleName()));
		
		String sensorDataProviderServiceClassName = arguments.getString(SensorDataProviderService.class.getSimpleName());
		try {
			bindSensorHistoryService();
			sensorDataProviderServiceClass = Class.forName(sensorDataProviderServiceClassName);
			bindSensorDataProviderService(sensorDataProviderServiceClass);
		} catch (ClassNotFoundException e) {
			Log.e(LOG_TAG, "Unable to bind SensorDataProviderService of class " + sensorDataProviderServiceClassName);
		}
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sensorhistory, container, false);
        configureGraph(rootView);
        return rootView;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		sensorDataProviderService.removeSensorValueListener(this, sensorType);

    	getActivity().unbindService(sensorDataProviderServiceConnection);
    	getActivity().unbindService(sensorHistoryServiceConnection);

    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
		
		plot.clear();
		sensorValueHistorySeries = null;
		plot = null;
	}
	
	@Override
	public void valueUpdate(Context context, SensorType sensorType, Double updatedValue) {
		if (this.sensorType == sensorType) {
			addToGraph(System.currentTimeMillis(), updatedValue);
		}		
	}
	
	private void configureGraph(View rootView) {
		String themeFromPreferences = ThemeUtil.getThemeFromPreferences(getActivity().getApplicationContext());

		plot = (XYPlot) rootView.findViewById(R.id.sensorValueHistoryPlot);
        plot.setDomainLabel("");
        plot.setRangeLabel("");
        
        plot.setDomainValueFormat(new TimeLabelFormat());
        
        addNewSensorValueSerieToPlot();
 
        plot.getGraphWidget().setDomainLabelOrientation(-90);
        plot.getGraphWidget().setDomainLabelVerticalOffset(-35);
        plot.getGraphWidget().getDomainLabelPaint().setTextAlign(Paint.Align.RIGHT);
        
        plot.setBorderStyle(BorderStyle.NONE, null, null);

        plot.setDomainStep(XYStepMode.SUBDIVIDE, 15);
        
        int baseColor;
        int highlightColor;
		if ("Dark".equals(themeFromPreferences)) {
			baseColor = Color.BLACK;
			highlightColor = Color.WHITE;
		} else {
			baseColor = Color.WHITE;
			highlightColor = Color.BLACK;
		}
        // Colors
        plot.getGraphWidget().getBackgroundPaint().setColor(baseColor);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(baseColor);
        plot.getGraphWidget().getDomainLabelPaint().setColor(highlightColor);
        plot.getGraphWidget().getRangeLabelPaint().setColor(highlightColor);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(highlightColor);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(highlightColor);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(highlightColor);

        // Remove legend
        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());
        plot.getLayoutManager().remove(plot.getTitleWidget());

        plot.getGraphWidget().setSize(new SizeMetrics(
                0, SizeLayoutType.FILL,
                0, SizeLayoutType.FILL));
	}

	private void addNewSensorValueSerieToPlot() {
		String themeFromPreferences = ThemeUtil.getThemeFromPreferences(getActivity().getApplicationContext());

		sensorValueHistorySeries = new SimpleXYSeries("SensorValue");
		
		int lineColor;
		if ("Dark".equals(themeFromPreferences)) {
			lineColor = Color.WHITE;
		} else {
			lineColor = Color.BLACK;
		}
		
        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter(lineColor, Color.TRANSPARENT, Color.TRANSPARENT, null);
        Paint paint = lineAndPointFormatter.getLinePaint();
        paint.setStrokeWidth(8);
        lineAndPointFormatter.setLinePaint(paint);
        plot.addSeries(sensorValueHistorySeries, lineAndPointFormatter);
	}
	
    private void bindSensorHistoryService() {	
    	Intent intent = new Intent(this.getActivity(), SensorValueHistoryServiceImpl.class);
    	boolean bindServiceSuccessFull = getActivity().bindService(intent, sensorHistoryServiceConnection, Context.BIND_AUTO_CREATE);
    	if (!bindServiceSuccessFull) {
    		Log.e(LOG_TAG, "Binding to HistoryService was not successfull");
    	}
    }
    
    private void bindSensorDataProviderService(Class<?> sensorDataProviderServiceClassToStart) {	
    	Intent intent = new Intent(this.getActivity(), sensorDataProviderServiceClassToStart);
    	boolean bindServiceSuccessFull = getActivity().bindService(intent, sensorDataProviderServiceConnection, Context.BIND_AUTO_CREATE);
    	if (!bindServiceSuccessFull) {
    		Log.e(LOG_TAG, "Binding to SensorDataProviderService was not successfull");
    	}
    }

	private void addDataFromHistory() {
		// Do not block the UI thread, by using an aSyncTask
		AsyncTask<Void,Void,List<SensorValueHistoryItem>> asyncTask = new AsyncTask<Void, Void, List<SensorValueHistoryItem>>() {
			@Override
			protected List<SensorValueHistoryItem> doInBackground(Void... params) {
				return sensorHistoryService.getAll(TemperatureHistoryFragment.this.getActivity().getApplicationContext(), sensorType);
			}
			@Override
			protected void onPostExecute(List<SensorValueHistoryItem> result) {
				for (SensorValueHistoryItem item : result) {
					addToGraph(item.getTimestamp(), item.getSensorValue());
				}
			}
		};
		asyncTask.execute();
	}

	private void addToGraph(long timestamp, Double value) {
		if (plot != null && value != null) {
			double valueInPrefereneUnit = TemperatureUtil.convertFromStorageUnitToPreferenceUnit(getActivity().getApplicationContext(), value);
			sensorValueHistorySeries.addLast(timestamp, valueInPrefereneUnit);

			double roundedValue = TemperatureUtil.round(value);
			updateMinMaxValues(roundedValue);
			setBoundaries();
			setRangeScale();
			
			plot.redraw();
		}
	}

	private void updateMinMaxValues(double roundedValue) {
		if (maxAddedValue == null || roundedValue > maxAddedValue) {
			maxAddedValue = roundedValue;
		}
		if (minAddedValue == null || roundedValue < minAddedValue) {
			minAddedValue = roundedValue;
		}
	}
	
	private void setBoundaries() {
		if (minAddedValue.doubleValue() == maxAddedValue.doubleValue()) {
			plot.setRangeBoundaries(minAddedValue - 0.1, maxAddedValue + 0.1, BoundaryMode.FIXED);
		} else {
			plot.setRangeBoundaries(minAddedValue, maxAddedValue, BoundaryMode.AUTO);
		}
	}

	private void setRangeScale() {
		double difference = maxAddedValue.doubleValue() - minAddedValue.doubleValue();
		if (difference == 0 || difference <= 1.0) { 
			plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
		} else {
			double step = difference / 14;
			double roundedStep = new BigDecimal(step).setScale(1, RoundingMode.HALF_UP).doubleValue();
			plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, roundedStep);
		}
	}
	
	/**
	 * Handles changes in the temperature source preference
	 */
	private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(preferenceTemperatureUnitKey)) {
				clear();
				addDataFromHistory();
			}
		}
	}
	
	private void clear() {
		plot.clear();
        addNewSensorValueSerieToPlot();
	}
	
	private final class TimeLabelFormat extends Format {
		private static final long serialVersionUID = 2204112458107503528L;

		private final DateFormat dateFormat = SimpleDateFormat.getTimeInstance();
        
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            long timestamp = ((Number) obj).longValue();
            Date date = new Date(timestamp);
            return dateFormat.format(date, toAppendTo, pos);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;
        }
	}
	
	private ServiceConnection sensorDataProviderServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			sensorDataProviderService = ((AbstractSensorDataProviderService.LocalBinder) service).getService();
			sensorDataProviderService.addSensorValueListener(TemperatureHistoryFragment.this, sensorType);
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			sensorDataProviderService.removeSensorValueListener(TemperatureHistoryFragment.this, sensorType);
		}
	};
	
    private ServiceConnection sensorHistoryServiceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName componentName, IBinder service) {    	
    		sensorHistoryService = ((SensorValueHistoryServiceImpl.LocalBinder) service).getService();
    		addDataFromHistory();
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName componentName) {
    		sensorHistoryService = null;
    	}
    };	
}
