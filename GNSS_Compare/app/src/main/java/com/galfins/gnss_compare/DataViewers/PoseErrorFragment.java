package com.galfins.gnss_compare.DataViewers;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.ui.Anchor;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.TableOrder;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
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
import java.util.Observable;
import java.util.Observer;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gnss_compare.R;

/**
 * Created by Mateusz Krainski on 31/03/2018.
 * This class is for...
 */

public class PoseErrorFragment extends Fragment implements DataViewer {
    /**
     * The number of max point to be plotted for a single data series
     */
    private static final int MAX_PLOTTED_POINTS = 200;

    /**
     * Plot type name
     */
    private static String PLOT_TYPE = "POSE_ERROR_PLOT";

    /**
     * Tag used for logging
     */
    private static final String TAG = "Positioning error plot";

    private Observer plotUpdater;

    /**
     * Displayed plot title
     */
    private static final String PLOT_DISPLAYED_NAME =  "Positioning error plot";
    private static final String PLOT_LABEL_X = "East [m]";
    private static final String PLOT_LABEL_Y = "North [m]";

    private boolean initalized = false;

    XYPlot plot;

    ArrayList<PoseErrorPlotDataSeries> data = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.pose_error_plot_page, container, false);

        plot = rootView.findViewById(R.id.plot);

        preformatPlot(plot);

        for(int i = 0; i< MainActivity.createdCalculationModules.size(); i++){
            synchronized (MainActivity.createdCalculationModules.get(i)) {
                try {
                    Iterator<PoseErrorPlotDataSeries> itr = data.iterator();
                    PoseErrorPlotDataSeries reference;

                    while(itr.hasNext()) {
                        reference = itr.next();
                        if (reference.getCalculationModuleReference() == MainActivity.createdCalculationModules.get(i)) {
                            MainActivity.createdCalculationModules.get(i).removeObserver(reference.getDataObserver());
                        }
                    }

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        initalized = true;

        for(int i=0; i<MainActivity.createdCalculationModules.size(); i++){
            synchronized (MainActivity.createdCalculationModules.get(i)) {
                addSeries(MainActivity.createdCalculationModules.get(i));
            }
        }

        return rootView;
    }

    @Override
    public void addSeries(CalculationModule calculationModule) {

        registerSeries(calculationModule);
        calculationModule.addObserver(getSeries(calculationModule).getDataObserver());

        if(initalized) {
            XYSeriesFormatter formatter = new LineAndPointFormatter(
                    null,
                    Color.argb(50,
                            Color.red(calculationModule.getDataColor()),
                            Color.green(calculationModule.getDataColor()),
                            Color.blue(calculationModule.getDataColor())),
                    Color.WHITE, //TODO: remove the black background from the legend
                    null);

            plot.addSeries(getSeries(calculationModule), formatter);
            formatSeries(plot, formatter);
        }
    }

    private PoseErrorPlotDataSeries getSeries(CalculationModule calculationModule){
        return data.get(getSeriesId(calculationModule));
    }

    private void registerSeries(CalculationModule calculationModule){
        if(!seriesRegistered(calculationModule)){
            data.add(new PoseErrorPlotDataSeries(calculationModule, MAX_PLOTTED_POINTS));
        }
    }

    private int getSeriesId(CalculationModule calculationModule){
        for(int i=0; i< data.size(); i++){
            if(data.get(i).getCalculationModuleReference() == calculationModule)
                return i;
        }
        return -1;
    }

    private boolean seriesRegistered(CalculationModule calculationModule) {

        for(CalculationModuleDataSeries series : data){
            if(series.getCalculationModuleReference() == calculationModule)
                return true;
        }

        return false;
    }

    /**
     * Removes the series associated with {@code calculationModule} from the plot
     * @param calculationModule reference to the calculation module
     */
    public void removeSeries(CalculationModule calculationModule){

        if(initalized) {
            Iterator<PoseErrorPlotDataSeries> itr = data.iterator();
            PoseErrorPlotDataSeries reference;

            while (itr.hasNext()) {
                reference = itr.next();
                if (reference.getCalculationModuleReference() == calculationModule) {
                    plot.removeSeries(reference);
                    itr.remove();
                    calculationModule.removeObserver(reference.getDataObserver());
                }
            }
        }
    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {

    }

    private void preformatPlot(XYPlot plot){

        double plotBoundary = (double)getResources().getInteger(R.integer.error_plot_range);

        final double[] PLOT_BOUNDARY_X = {-plotBoundary, plotBoundary};
        final double[] PLOT_BOUNDARY_Y = {-plotBoundary, plotBoundary};
        final double PLOT_STEP = plotBoundary/5.0;

        plot.setTitle(PLOT_DISPLAYED_NAME);

        plot.setRangeBoundaries(PLOT_BOUNDARY_X[0], PLOT_BOUNDARY_X[1], BoundaryMode.FIXED);
        plot.setDomainBoundaries(PLOT_BOUNDARY_Y[0], PLOT_BOUNDARY_Y[1], BoundaryMode.FIXED);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, PLOT_STEP);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, PLOT_STEP);
        plot.getRangeTitle().setText(PLOT_LABEL_Y);
       // plot.setDomainLabel(PLOT_LABEL_X);
        plot.getDomainTitle().setText(PLOT_LABEL_X);
        plot.getDomainTitle().position(0, HorizontalPositioning.ABSOLUTE_FROM_CENTER, -0.2f, VerticalPositioning.RELATIVE_TO_BOTTOM, Anchor.BOTTOM_MIDDLE);

        LineAndPointRenderer pointRenderer = plot.getRenderer(LineAndPointRenderer.class);

        plot.getLegend().setTableModel(new DynamicTableModel(3, 4, TableOrder.ROW_MAJOR));

        plot.getGraph().setSize(new Size(
                0.65f, SizeMode.RELATIVE,
                0.99f, SizeMode.RELATIVE));

        plot.getLegend().setSize(new Size(
                0.2f, SizeMode.RELATIVE,
                0.7f, SizeMode.RELATIVE));

        plot.getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getBorderPaint().setColor(Color.TRANSPARENT);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraph().getDomainGridLinePaint().setColor(Color.LTGRAY);
        plot.getGraph().getRangeGridLinePaint().setColor(Color.LTGRAY);

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

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
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
        ((LineAndPointFormatter)formatter).getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
    }

    /**
     * A data series implementation, which extracts signal strengths for satellites observed in a
     * CalculationModule, to which the reference is passed in constructor.
     */
    private class PoseErrorPlotDataSeries implements DataViewer.CalculationModuleDataSeries, XYSeries {

        /**
         * The number of max point to be plotted for a single data series
         */
        private final int MAX_PLOTTED_POINTS;

        /**
         * Historical poses, which are to be plotted
         */
        private ArrayList<Double[]> registeredPoses;

        /**
         * Reference to the calculation module linked with this data series object.
         */
        private CalculationModule calculationModuleReference;

        PoseErrorPlotDataSeries(CalculationModule calculationModule, int maxPlottedPoints) {
            calculationModuleReference = calculationModule;
            registeredPoses = new ArrayList<>();
            MAX_PLOTTED_POINTS = maxPlottedPoints;
        }

        /**
         * @return created data observer, which is to be added to the calculation module
         */
        Observer getDataObserver() {
            return new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    Coordinates calculatedPose = calculationModuleReference.getPose();
                    Location phoneInternalPose = calculationModuleReference.getLocationFromGoogleServices();

                    if(phoneInternalPose!=null && calculatedPose!=null) {

                        double[] poseError = Coordinates.deltaGeodeticToDeltaMeters(
                                phoneInternalPose.getLatitude(),
                                phoneInternalPose.getAltitude(),
                                (calculatedPose.getGeodeticLatitude() - phoneInternalPose.getLatitude()) * Math.PI / 180.0,
                                (calculatedPose.getGeodeticLongitude() - phoneInternalPose.getLongitude()) * Math.PI / 180.0);

                        Log.d(TAG, "update: pose error: " + poseError[0] + ", " + poseError[1]);

                        registeredPoses.add(new Double[]{poseError[0], poseError[1]});
                        if (registeredPoses.size() > MAX_PLOTTED_POINTS)
                            registeredPoses.remove(0);

                        if (initalized) {
                            plot.redraw();
                        }
                    }
                }
            };
        }

        @Override
        public CalculationModule getCalculationModuleReference() {
            return calculationModuleReference;
        }

        @Override
        public int size() {
            return Math.min(registeredPoses.size(), MAX_PLOTTED_POINTS);
        }

        @Override
        public Number getX(int index) {
            if (index >= registeredPoses.size()) {
                throw new IllegalArgumentException();
            }
            try {
                return registeredPoses.get(index)[1];
            } catch (NullPointerException e) {
                return null;
            }
        }

        @Override
        public Number getY(int index) {
            if (index >= registeredPoses.size()) {
                throw new IllegalArgumentException();
            }
            try {
                return registeredPoses.get(index)[0];
            } catch (NullPointerException e) {
                return null;
            }
        }

        @Override
        public String getTitle() {
            return calculationModuleReference.getName();
        }
    }
}
