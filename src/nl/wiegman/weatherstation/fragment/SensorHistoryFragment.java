package nl.wiegman.weatherstation.fragment;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nl.wiegman.weatherstation.MainActivity;
import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.history.SensorValueHistoryItem;
import nl.wiegman.weatherstation.history.TemperatureHistory;
import nl.wiegman.weatherstation.sensorvaluelistener.TemperatureValueChangeListener;
import nl.wiegman.weatherstation.util.TemperatureUtil;

import org.apache.commons.lang3.time.DateUtils;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
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
 * Fragment containing a graph that shows the sensor values over time
 */
public class SensorHistoryFragment extends Fragment implements TemperatureValueChangeListener {

	private XYPlot plot;
	private SimpleXYSeries sensorValueHistorySeries;

	private Double maxAddedValue = null;
	private Double minAddedValue = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sensorhistory, container, false);

        configureGraph(rootView);
        
        addDataFromHistory();
        
        ((MainActivity)getActivity()).addTemperatureValueChangeListener(this);
        
        return rootView;
	}

	private void configureGraph(View rootView) {
		plot = (XYPlot) rootView.findViewById(R.id.sensorValueHistoryPlot);
        plot.setDomainLabel("");
        plot.setRangeLabel("");
        
        plot.setDomainValueFormat(new TimeLabelFormat());
        
        sensorValueHistorySeries = new SimpleXYSeries("SensorValue");

        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, null);
        Paint paint = lineAndPointFormatter.getLinePaint();
        paint.setStrokeWidth(8);
        lineAndPointFormatter.setLinePaint(paint);
        plot.addSeries(sensorValueHistorySeries, lineAndPointFormatter);
 
        plot.getGraphWidget().setDomainLabelOrientation(-90);
        plot.getGraphWidget().setDomainLabelVerticalOffset(-35);
        plot.getGraphWidget().getDomainLabelPaint().setTextAlign(Paint.Align.RIGHT);
        
        plot.setBorderStyle(BorderStyle.NONE, null, null);

        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1d);
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
	
	private void addDataFromHistory() {
		TemperatureHistory historyStore = new TemperatureHistory();
		List<SensorValueHistoryItem> history = historyStore.getAll(getActivity());
		
		for (SensorValueHistoryItem item : history) {
			addToGraph(item.getTimestamp(), item.getSensorValue());
		}
	}
	
	@Override
	public void temperatureChanged(Context context, Double updatedTemperature) {
		addToGraph(System.currentTimeMillis(), updatedTemperature);
	}
	
	private void addToGraph(long timestamp, Double value) {
		
		if (plot != null && value != null) {
			double roundedValue = TemperatureUtil.round(value);
			
			Date timestampRoundedOnSecond = DateUtils.round(new Date(timestamp), Calendar.SECOND);
			long time = timestampRoundedOnSecond.getTime();
			
			sensorValueHistorySeries.addLast(time, TemperatureUtil.round(roundedValue));

			if (maxAddedValue == null || roundedValue > maxAddedValue) {
				maxAddedValue = roundedValue;
			}
			if (minAddedValue == null || roundedValue < minAddedValue) {
				minAddedValue = roundedValue;
			}
			
			if (minAddedValue.doubleValue() == maxAddedValue.doubleValue()) {
				plot.setRangeBoundaries(minAddedValue - 0.1, maxAddedValue + 0.1, BoundaryMode.FIXED);
			} else {
				plot.setRangeBoundaries(minAddedValue, maxAddedValue, BoundaryMode.AUTO);
			}
			
			plot.redraw();
		}
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
