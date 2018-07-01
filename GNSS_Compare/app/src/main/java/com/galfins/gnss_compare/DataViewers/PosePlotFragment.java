package com.galfins.gnss_compare.DataViewers;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.TableOrder;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesFormatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gnss_compare.R;

/**
 * Created by Mateusz Krainski on 25/03/2018.
 * This class is for...
 */

public class PosePlotFragment extends Fragment implements DataViewer {

    /**
     * The number of max point to be plotted for a single data series
     */
    private static final int MAX_PLOTTED_POINTS = 1000;

    /**
     * Plot type name
     */
    private static String PLOT_TYPE = "POWER_PLOT";

    private Observer plotUpdater;

    /**
     * Displayed plot title
     */
    private static final String PLOT_DISPLAYED_NAME = "Pose plot";

    /**
     * Y plot limits
     */
    private final double[] SNR_PLOT_Y_LIMITS = {0.0, 50.0};


    private final int DATA_LENGTH = 10;

    private boolean initalized = false;

    XYPlot plot;

    ArrayList<PosePlotDataSeries> data = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.pose_plot_page, container, false);

        plot = rootView.findViewById(R.id.plot);

        preformatPlot(plot);

        for(int i = 0; i< MainActivity.createdCalculationModules.size(); i++){
            synchronized (MainActivity.createdCalculationModules.get(i)) {
                try {
                    Iterator<PosePlotDataSeries> itr = data.iterator();
                    PosePlotDataSeries reference;

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

    private PosePlotDataSeries getSeries(CalculationModule calculationModule){
        return data.get(getSeriesId(calculationModule));
    }

    private void registerSeries(CalculationModule calculationModule){
        if(!seriesRegistered(calculationModule)){
            data.add(new PosePlotDataSeries(calculationModule, MAX_PLOTTED_POINTS));
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
            Iterator<PosePlotDataSeries> itr = data.iterator();
            PosePlotDataSeries reference;

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
        final double[] PLOT_BOUNDARY_X = {51.0, 53.0};
        final double[] PLOT_BOUNDARY_Y = {3.0, 5.0};
//        final double PLOT_STEP = 2.0;

        plot.setTitle(PLOT_DISPLAYED_NAME);

        plot.setRangeBoundaries(PLOT_BOUNDARY_X[0], PLOT_BOUNDARY_X[1], BoundaryMode.FIXED);
        plot.setDomainBoundaries(PLOT_BOUNDARY_Y[0], PLOT_BOUNDARY_Y[1], BoundaryMode.FIXED);

        LineAndPointRenderer pointRenderer = plot.getRenderer(LineAndPointRenderer.class);

        plot.getLegend().setTableModel(new DynamicTableModel(3, 4, TableOrder.ROW_MAJOR));

        plot.getGraph().setSize(new Size(
                0.7f, SizeMode.RELATIVE,
                1.0f, SizeMode.RELATIVE));

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
    }

    protected void formatSeries(XYPlot plot, XYSeriesFormatter formatter) {
        ((LineAndPointFormatter)formatter).getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
    }

    /**
     * A data series implementation, which extracts signal strengths for satellites observed in a
     * CalculationModule, to which the reference is passed in constructor.
     */
    private class PosePlotDataSeries implements DataViewer.CalculationModuleDataSeries, XYSeries {

        /**
         * The number of max point to be plotted for a single data series
         */
        private final int MAX_PLOTTED_POINTS;

        /**
         * Historical poses, which are to be plotted
         */
        private ArrayList<Coordinates> registeredPoses;

        /**
         * Reference to the calculation module linked with this data series object.
         */
        private CalculationModule calculationModuleReference;

        PosePlotDataSeries(CalculationModule calculationModule, int maxPlottedPoints) {
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
                    registeredPoses.add(calculationModuleReference.getPose());
                    if (registeredPoses.size() > MAX_PLOTTED_POINTS)
                        registeredPoses.remove(0);

                    if(initalized) {
                        plot.redraw();
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
                return registeredPoses.get(index).getGeodeticLongitude();
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
                return registeredPoses.get(index).getGeodeticLatitude();
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
