package nl.wiegman.weatherstation.fragment;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.util.TemperatureUtil;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.Plot.BorderStyle;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

public abstract class SensorHistoryFragment extends Fragment {

	private String LOG_TAG = this.getClass().getSimpleName();
	
	private XYPlot plot;
	private SimpleXYSeries sensorValueHistorySeries;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(LOG_TAG, "onCreate sensorHistoryFragment");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sensorhistory, container, false);

		// initialize our XYPlot reference:
        plot = (XYPlot) rootView.findViewById(R.id.sensorValueHistoryPlot);
        plot.setDomainLabel("");
        plot.setRangeLabel("");
        plot.setTitle(getTitle());
        
        plot.setDomainValueFormat(new TimeLabelFormat());
        
        sensorValueHistorySeries = new SimpleXYSeries("Temperature");

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
        
        // Colors
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        //Remove legend
        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());
        plot.getLayoutManager().remove(plot.getTitleWidget());

        plot.getGraphWidget().setSize(new SizeMetrics(
                0, SizeLayoutType.FILL,
                0, SizeLayoutType.FILL));

        return rootView;
	}
		
	protected abstract String getTitle();

	public void valueChanged(Double updatedTemperature) {
		if (sensorValueHistorySeries != null && plot != null) {
			sensorValueHistorySeries.addLast(System.currentTimeMillis(), TemperatureUtil.round(updatedTemperature));
			plot.redraw();
		}
	}
	
	private final class TimeLabelFormat extends Format {
		private static final long serialVersionUID = 2204112458107503528L;

		private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
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