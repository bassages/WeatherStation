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
import nl.wiegman.weatherstation.history.SensorValueHistoryItem;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
public abstract class AbstractTemperatureHistoryFragment extends Fragment {
	private XYPlot plot;
	private SimpleXYSeries sensorValueHistorySeries;

	private Double maxAddedValue = null;
	private Double minAddedValue = null;
	
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    
	private String preferenceTemperatureUnitKey;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
    	preferenceListener = new PreferenceListener();	
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		preferenceTemperatureUnitKey = getActivity().getApplicationContext().getString(R.string.preference_temperature_unit_key);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sensorhistory, container, false);
        configureGraph(rootView);
        addDataFromHistory();
        return rootView;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
		
		plot.clear();
		sensorValueHistorySeries = null;
		plot = null;
	}
	
	private void configureGraph(View rootView) {
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
        
        // Colors
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

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
		sensorValueHistorySeries = new SimpleXYSeries("SensorValue");
        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, null);
        Paint paint = lineAndPointFormatter.getLinePaint();
        paint.setStrokeWidth(8);
        lineAndPointFormatter.setLinePaint(paint);
        plot.addSeries(sensorValueHistorySeries, lineAndPointFormatter);
	}
	
	private void addDataFromHistory() {
		// Do not block the UI thread, by using an aSyncTask
		AsyncTask<Void,Void,List<SensorValueHistoryItem>> asyncTask = new AsyncTask<Void, Void, List<SensorValueHistoryItem>>() {
			@Override
			protected List<SensorValueHistoryItem> doInBackground(Void... params) {
				return getHistoryItems();
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

	protected abstract List<SensorValueHistoryItem> getHistoryItems();
		
	protected void addToGraph(long timestamp, Double value) {
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
}
