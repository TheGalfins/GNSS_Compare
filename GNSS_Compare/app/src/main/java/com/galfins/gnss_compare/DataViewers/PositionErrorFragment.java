/*
 * Copyright 2018 TFI Systems

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gnss_compare.CalculationModulesArrayList;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gnss_compare.R;
import com.google.common.collect.Sets;

/**
 * Created by Mateusz Krainski on 31/03/2018.
 * This class is for...
 */

public class PositionErrorFragment extends Fragment implements DataViewer {
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

    /**
     * Stores the phone's FINE location. Used as reference when calculating
     * the positioning error
     */
    Location phoneInternalPosition;

    /**
     * Displayed plot title
     */
    private static final String PLOT_DISPLAYED_NAME =  "Positioning error plot";
    private static final String PLOT_LABEL_X = "East [m]";
    private static final String PLOT_LABEL_Y = "North [m]";

    private boolean initialized = false;

    XYPlot plot;

    Map<CalculationModule, PoseErrorPlotDataSeries> data = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.pose_error_plot_page, container, false);

        plot = rootView.findViewById(R.id.plot);

        preformatPlot(plot);

        for(Map.Entry<CalculationModule, PoseErrorPlotDataSeries> entry : data.entrySet() ) {

            XYSeriesFormatter formatter = new LineAndPointFormatter(
                    null,
                    Color.argb(50,
                            Color.red(entry.getKey().getDataColor()),
                            Color.green(entry.getKey().getDataColor()),
                            Color.blue(entry.getKey().getDataColor())),
                    Color.WHITE, //TODO: remove the black background from the legend
                    null);

            plot.addSeries(entry.getValue(), formatter);

            formatSeries(formatter);
        }

        initialized = true;

        return rootView;
    }

    public void addSeries(CalculationModule calculationModule) {

        if(initialized) {

            data.put(calculationModule, new PoseErrorPlotDataSeries(MAX_PLOTTED_POINTS));

            XYSeriesFormatter formatter = new LineAndPointFormatter(
                    null,
                    Color.argb(50,
                            Color.red(calculationModule.getDataColor()),
                            Color.green(calculationModule.getDataColor()),
                            Color.blue(calculationModule.getDataColor())),
                    Color.WHITE, //TODO: remove the black background from the legend
                    null);

            plot.addSeries(data.get(calculationModule), formatter);

            formatSeries(formatter);
        }
    }

    /**
     * Removes the series associated with {@code calculationModule} from the plot
     * @param calculationModule reference to the calculation module
     */
    public void removeSeries(CalculationModule calculationModule){

        if(initialized) {
            plot.removeSeries(data.get(calculationModule));
            data.remove(calculationModule);
        }

    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {
        phoneInternalPosition = location;
    }

    @Override
    public void update(CalculationModulesArrayList calculationModules) {

        if(initialized) {
            // modules to be added
            for (CalculationModule calculationModule : Sets.difference(
                    new HashSet<>(calculationModules),
                    data.keySet())) {
                addSeries(calculationModule);
            }

            // modules to be removed
            for (CalculationModule calculationModule : Sets.difference(
                    data.keySet(),
                    new HashSet<>(calculationModules))) {
                removeSeries(calculationModule);
            }
        }

        for(Map.Entry<CalculationModule, PoseErrorPlotDataSeries> entry: data.entrySet()){
            entry.getValue().update(entry.getKey());
        }

        if(plot!=null)
            plot.redraw();
    }

    @Override
    public void updateOnUiThread(CalculationModulesArrayList calculationModules) {

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

    protected void formatSeries(XYSeriesFormatter formatter) {
        ((LineAndPointFormatter)formatter).getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
    }

    /**
     * A data series implementation, which extracts signal strengths for satellites observed in a
     * CalculationModule, to which the reference is passed in constructor.
     */
    private class PoseErrorPlotDataSeries implements XYSeries {

        /**
         * The number of max point to be plotted for a single data series
         */
        private final int MAX_PLOTTED_POINTS;

        /**
         * Historical poses, which are to be plotted
         */
        private ArrayList<Double[]> registeredPoses;

        private String seriesName;

        PoseErrorPlotDataSeries(int maxPlottedPoints) {
            registeredPoses = new ArrayList<>();
            MAX_PLOTTED_POINTS = maxPlottedPoints;
        }

        public void update(CalculationModule calculationModule){
            Coordinates calculatedPose = calculationModule.getPose();
//            Location phoneInternalPose = calculationModule.getLocationFromGoogleServices();
            seriesName = calculationModule.getName();

            if(phoneInternalPosition!=null && calculatedPose!=null) {

                double[] poseError = Coordinates.deltaGeodeticToDeltaMeters(
                        phoneInternalPosition.getLatitude(),
                        phoneInternalPosition.getAltitude(),
                        (calculatedPose.getGeodeticLatitude() - phoneInternalPosition.getLatitude()) * Math.PI / 180.0,
                        (calculatedPose.getGeodeticLongitude() - phoneInternalPosition.getLongitude()) * Math.PI / 180.0);

                Log.d(TAG, "update: pose error: " + poseError[0] + ", " + poseError[1]);

                calculationModule.getPvtMethod().logError(poseError[0], poseError[1]);

                registeredPoses.add(new Double[]{poseError[0], poseError[1]});
                if (registeredPoses.size() > MAX_PLOTTED_POINTS)
                    registeredPoses.remove(0);

                if (initialized) {
                    plot.redraw();
                }
            }
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
            return seriesName;
        }
    }
}
