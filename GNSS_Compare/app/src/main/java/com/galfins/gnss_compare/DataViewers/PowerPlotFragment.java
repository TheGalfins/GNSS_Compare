package com.galfins.gnss_compare.DataViewers;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.ui.SeriesRenderer;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesFormatter;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gnss_compare.Constellations.SatelliteParameters;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gnss_compare.R;

import static android.location.GnssStatus.CONSTELLATION_GALILEO;
import static android.location.GnssStatus.CONSTELLATION_GPS;
import static android.location.GnssStatus.CONSTELLATION_UNKNOWN;

/**
 * Created by Mateusz Krainski on 25/03/2018.
 * This class is for...
 */

public class PowerPlotFragment extends Fragment implements DataViewer {

    /**
     * Plot type name
     */
    private static String PLOT_TYPE = "POWER_PLOT";

    private Observer plotUpdater = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            if(initialized) {
                data.update();
                plot.redraw();
            }
        }
    };

    /**
     * Displayed plot title
     */
    private static final String PLOT_DISPLAYED_NAME = "Satellite signal strength";

    /**
     * Y plot limits
     */
    private final int[] SNR_PLOT_Y_LIMITS = {0, 50};


    private final int DATA_LENGTH = 20;

    XYPlot plot;

    PowerPlotDataSeries data = new PowerPlotDataSeries();

    private boolean initialized = false;

    private PowerBarFormatter gpsFormatter;
    private PowerBarFormatter galileoFormatter;
    private PowerBarFormatter defaultFormatter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.power_plot_page, container, false);

        plot = rootView.findViewById(R.id.plot);

        gpsFormatter = new PowerBarFormatter(
                ContextCompat.getColor(getActivity(), R.color.gpsGrey),
                Color.BLACK);

        galileoFormatter = new PowerBarFormatter(
                ContextCompat.getColor(getActivity(), R.color.galileoBlue),
                Color.BLACK);

        defaultFormatter  = new PowerBarFormatter(
                ContextCompat.getColor(getActivity(), R.color.unknownSatelliteRed),
                Color.BLACK);

        preformatPlot(plot);

        for(int i=0; i<MainActivity.createdCalculationModules.size(); i++){
            synchronized (MainActivity.createdCalculationModules.get(i)) {
                try {
                    MainActivity.createdCalculationModules.get(i).removeObserver(plotUpdater);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        initialized = true;

        XYSeriesFormatter formatter = new PowerBarFormatter(Color.BLUE, Color.BLACK);
        plot.addSeries(data, formatter);
        formatSeries(plot, formatter);

        for(int i=0; i<MainActivity.createdCalculationModules.size(); i++){
            synchronized (MainActivity.createdCalculationModules.get(i)) {
                addSeries(MainActivity.createdCalculationModules.get(i));
            }
        }

        return rootView;
    }

    @Override
    public void addSeries(CalculationModule calculationModule) {
        data.addSeries(calculationModule);
        calculationModule.addObserver(plotUpdater);
    }


    /**
     * Removes the series associated with {@code calculationModule} from the plot
     * @param calculationModule reference to the calculation module
     */
    @Override
    public void removeSeries(CalculationModule calculationModule){
        data.removeSeries(calculationModule);
    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {

    }


    private void preformatPlot(XYPlot plot){
        final double SNR_PLOT_X_EXTENSION = 0.0;

        plot.setTitle(PLOT_DISPLAYED_NAME);

        plot.setRangeBoundaries(SNR_PLOT_Y_LIMITS[0], SNR_PLOT_Y_LIMITS[1], BoundaryMode.FIXED);
        plot.setDomainBoundaries(-SNR_PLOT_X_EXTENSION, DATA_LENGTH+1, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1);
        plot.getRangeTitle().setText("Carrier-to-noise density [dB-Hz]");

        plot.getGraph().setSize(new Size(
                0.7f, SizeMode.RELATIVE,
                0.99f, SizeMode.RELATIVE));


        plot.getLegend().setVisible(false);

        plot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getBorderPaint().setColor(Color.TRANSPARENT);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getRangeGridLinePaint().setColor(Color.LTGRAY);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                int i = Math.round(((Number) obj).intValue());
                if((i-1)<data.satellites.size() && i>0)
                    return toAppendTo.append(data.satellites.get(i-1).getUniqueSatId());
                else
                    return toAppendTo.append("");
            }
            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });


        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                int i = Math.round(((Number) obj).intValue());
                return toAppendTo.append(i);
            }
            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });
    }

    protected void formatSeries(XYPlot plot, XYSeriesFormatter formatter) {

        BarRenderer barRenderer = plot.getRenderer(PowerBarRenderer.class);
        barRenderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_WIDTH, PixelUtils.dpToPix(15));
        barRenderer.setBarOrientation(BarRenderer.BarOrientation.SIDE_BY_SIDE);

        ((BarFormatter) formatter).setMarginLeft(PixelUtils.dpToPix(1));
        ((BarFormatter) formatter).setMarginRight(PixelUtils.dpToPix(1));
    }


    /**
     * A data series implementation, which extracts signal strengths for satellites observed in a
     * CalculationModule, to which the reference is passed in constructor.
     */
    private class PowerPlotDataSeries implements XYSeries {

        /**
         * reference to the calculation module
         */
        private Set<CalculationModule> registeredCalculationModules = new ArraySet<>();
        private List<SatelliteParameters> satellites = new ArrayList<>();

        /**
         *
         */
        PowerPlotDataSeries(){
//            registeredCalculationModules.add(calculationModule);
        }

        void addSeries(CalculationModule calculationModule){
            registeredCalculationModules.add(calculationModule);
        }

        public void removeSeries(CalculationModule calculationModule){

            for (Iterator<CalculationModule> iterator = registeredCalculationModules.iterator(); iterator.hasNext();) {
                CalculationModule reference = iterator.next();
                if (reference == calculationModule) {
                    // Remove the current element from the iterator and the list.
                    iterator.remove();
                    calculationModule.removeObserver(plotUpdater);
                }
            }
        }

        @Override
        public int size() {
            return DATA_LENGTH;
        }

        @Override
        public Number getX(int index) {
            if (index >= size())
                throw new IllegalArgumentException();
            return index+1;
        }

        @Override
        public Number getY(int index) {
            if (index >= size())
                throw new IllegalArgumentException();
            else if (index < satellites.size())
                return satellites.get(index).getSignalStrength();
            else
                return 0;
        }

        public int getType(int index){
            if (index >= size())
                throw new IllegalArgumentException();
            else if (index < satellites.size())
                return satellites.get(index).getConstellationType();
            else
                return CONSTELLATION_UNKNOWN;
        }

        @Override
        public String getTitle() {
            return "This should be removed";
        }

        public void update() {
            satellites.clear();

            for(CalculationModule calculationModule : registeredCalculationModules){
                for (int i=0; i<calculationModule.getConstellation().getUsedConstellationSize(); i++){

                    // not adding satellites already registered (this is a cross check if a
                    // satellite can be selected from multiple constellations (e.g. a Galileo satellite
                    // will be present in both Galileo and GPS+Galileo constellations.
                    boolean satelliteFound = false;

                    for(SatelliteParameters sat : satellites){

                        if(sat.getUniqueSatId() != null) {
                            if (sat.getUniqueSatId().equals(calculationModule.getConstellation().getSatellite(i).getUniqueSatId())) {
                                satelliteFound = true;
                                break;
                            }
                        }
                    }
                    if(!satelliteFound)
                        satellites.add(calculationModule.getConstellation().getSatellite(i));
                }
            }
        }
    }

    class PowerBarFormatter extends BarFormatter{
        public PowerBarFormatter(int fillColor, int borderColor){
            super(fillColor, borderColor);
        }

        @Override
        public Class<? extends SeriesRenderer> getRendererClass() {
            return PowerBarRenderer.class;
        }

        @Override
        public SeriesRenderer doGetRendererInstance(XYPlot plot) {
            return new PowerBarRenderer(plot);
        }
    }

    class PowerBarRenderer extends BarRenderer<PowerBarFormatter> {

        public PowerBarRenderer(XYPlot plot) {
            super(plot);
        }

        /**
         * Implementing this method to allow us to inject our
         * special selection getFormatter.
         * @param index index of the point being rendered.
         * @param series XYSeries to which the point being rendered belongs.
         * @return
         */
        @Override
        public PowerBarFormatter getFormatter(int index, XYSeries series) {
            switch (((PowerPlotDataSeries) series).getType(index)){
                case CONSTELLATION_GPS:
                    return gpsFormatter;
                case CONSTELLATION_GALILEO:
                    return galileoFormatter;
                default:
                    return defaultFormatter;
            }
        }
    }
}
