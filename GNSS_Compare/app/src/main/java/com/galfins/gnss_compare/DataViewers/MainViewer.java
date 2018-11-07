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

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gnss_compare.CalculationModulesArrayList;
import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.R;
import com.google.common.collect.Sets;

/**
 * Created by Mateusz Krainski on 04/05/2018.
 * This is the main data viewer, which displays data in a text form.
 */
public class MainViewer extends Fragment implements DataViewer {

    double nameColumnWidth = 0.0;
    double itemColumnWidth = 0.0;

    private final String TAG = this.getClass().getSimpleName();

    /**
     * CalculationGridItem is used as an interface for dynamically updated items within a GridLayout
     */
    private interface CalculationGridItem {
        /**
         * Creates views and stores them into the layout
         * @param gridLayout Layout to which the views are to be assigned
         */
        void reinitializeViews(GridLayout gridLayout);

        /**
         * decrements row number on which this item is stored. Used when another item (above current one)
         * is removed from the grid layout
         */
        void decremntRowId();

        /**
         * @return row id of item
         */
        int getRowId();

        /**
         * Removes item from grid
         * @param grid layout to which the item was assigned
         */
        void removeFromGrid(GridLayout grid);

        void update(CalculationModule calculationModule);
    }

    /**
     * Item (row) in the constellation grid layout
     */
    private class ConstellationItem {

        private final static int POSE_NAME_COLUMN = 0;
        private final static int POSE_VISIBLE_COLUMN= 1;
        private final static int POSE_USED_COLUMN = 2;

        /**
         * Width of the text fields on this grid
         */
        private final int []TEXT_FIELD_WIDTH = {
                (int) nameColumnWidth,
                (int) itemColumnWidth,
                (int) itemColumnWidth,
                (int) itemColumnWidth,
        };

        /**
         * view storing the name of the calculation module
         */
        private TextView nameView;

        /**
         * view storing the number of visible satellites
         */
        private TextView visibleView;

        /**
         * View storing the number of used satellites
         */
        private TextView usedView;

        /**
         * row id associated with this item.
         */
        private int rowId;

        /**
         * Updates the views with current values from the {@code constellationReference}
         */
        private void updateViews(Constellation constellationReference) {

            if(nameView != null &&
                    visibleView != null &&
                    usedView != null) {

                nameView.setText(constellationReference.getName());
                visibleView.setText(String.format("%d", constellationReference.getVisibleConstellationSize()));
                usedView.setText(String.format("%d", constellationReference.getUsedConstellationSize()));
            }
        }

        /**
         * Updates the views with current values from the {@code constellationReference}
         */
        private void initializeViewsAsEmpty() {

            if(nameView != null &&
                    visibleView != null &&
                    usedView != null) {

                nameView.setText("--");
                visibleView.setText("--");
                usedView.setText("--");
            }
        }

        /**
         *  @param gridLayout layout to which the item is to be assigned to
         * @param rowId id of the row to which the item is to be assigned.
         */
        public ConstellationItem(GridLayout gridLayout, int rowId){

            this.rowId = rowId;
            reinitializeViews(gridLayout);
        }

        public void reinitializeViews(GridLayout gridLayout) {
            if(getActivity() != null) {

                removeFromGrid(gridLayout);

                nameView = new TextView(getActivity());
                visibleView = new TextView(getActivity());
                usedView = new TextView(getActivity());

                initializeTextView(nameView, gridLayout, POSE_NAME_COLUMN);
                initializeTextView(visibleView, gridLayout, POSE_VISIBLE_COLUMN);
                initializeTextView(usedView, gridLayout, POSE_USED_COLUMN);

                initializeViewsAsEmpty();
            }
        }

        /**
         * Factory method to initialize each text view and add it to the layout
         * @param view text view to be initialized
         * @param layout layout to which the view is to be added
         * @param columnId column of the layout to which the view is to be added
         */
        private void initializeTextView(
                TextView view,
                GridLayout layout,
                int columnId){

            layout.addView(view);

            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.height = GridLayout.LayoutParams.WRAP_CONTENT;
            param.width = GridLayout.LayoutParams.WRAP_CONTENT;
            param.rightMargin = 5;
            param.topMargin = 5;
            view.setTextSize(11);
            view.setWidth(TEXT_FIELD_WIDTH[columnId]);
            view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            param.setGravity(Gravity.CENTER);
            param.columnSpec = GridLayout.spec(columnId);
            param.rowSpec = GridLayout.spec(rowId);
            view.setLayoutParams (param);
        }

        public void removeFromGrid(GridLayout grid) {
            grid.removeView(nameView);
            grid.removeView(visibleView);
            grid.removeView(usedView);
        }
    }

    private class ConstellationGrid{

        GridLayout referenceToLayout;
        Map<String, ConstellationItem> items = new HashMap<>();
        boolean initialized = false;

        private void initialize(){

            // todo: assess better if this race condition is true
            if(Constellation.getRegistered().size() > 0) {
                for (String key : Constellation.getRegistered()) {
                    try {
                        Constellation constellation = Constellation.getClassByName(key).newInstance();
                        referenceToLayout.setRowCount(referenceToLayout.getRowCount() + 1);
                        items.put(constellation.getName(), new ConstellationItem(referenceToLayout, items.size() + 1));
                    } catch (java.lang.InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                initialized = true;
            }
        }

        private void clearGrid(){
            for(Map.Entry<String, ConstellationItem> item : items.entrySet())
                item.getValue().removeFromGrid(referenceToLayout);

            items.clear();
        }

        public ConstellationGrid(GridLayout layout){
            referenceToLayout = layout;

            initialize();
        }

        public void reinitialize(GridLayout constellationGridView) {
            clearGrid();

            referenceToLayout = constellationGridView;

            initialize();
        }

        public void update(CalculationModulesArrayList calculationModules){

            if(!initialized)
                initialize();
            else {
                ConstellationItem item;
                for (CalculationModule calculationModule : calculationModules) {
                    item = items.get(calculationModule.getConstellation().getName());

                    if (item != null)
                        item.updateViews(calculationModule.getConstellation());
                }
            }

        }

    }

    /**
     * Item in the pose grid
     */
    private class PoseItem implements CalculationGridItem {

        private final static int POSE_NAME_COLUMN = 0;
        private final static int POSE_LAT_COLUMN = 1;
        private final static int POSE_LON_COLUMN = 2;
        private final static int POSE_ALT_COLUMN = 3;
        private final static int POSE_CLOCK_BIAS_COLUMN = 4;

        private final int []TEXT_FIELD_WIDTH = {
                (int) nameColumnWidth,
                (int) itemColumnWidth,
                (int) itemColumnWidth,
                (int) itemColumnWidth,
                (int) itemColumnWidth
        };

        private TextView nameView;
        private TextView latView;
        private TextView lonView;
        private TextView altView;
        private TextView clockBiasView;

        private int rowId;

        public PoseItem(GridLayout gridLayout, int rowId){

            this.rowId = rowId;

            reinitializeViews(gridLayout);
        }

        @Override
        public void reinitializeViews(GridLayout gridLayout){
            if(getActivity() != null) {

                removeFromGrid(gridLayout);

                nameView = new TextView(getActivity());
                latView = new TextView(getActivity());
                lonView = new TextView(getActivity());
                altView = new TextView(getActivity());
                clockBiasView = new TextView(getActivity());

                initializeTextView(nameView, gridLayout, POSE_NAME_COLUMN);
                initializeTextView(latView, gridLayout, POSE_LAT_COLUMN);
                initializeTextView(lonView, gridLayout, POSE_LON_COLUMN);
                initializeTextView(altView, gridLayout, POSE_ALT_COLUMN);
                initializeTextView(clockBiasView, gridLayout, POSE_CLOCK_BIAS_COLUMN);
            }
        }

        private void initializeTextView(
                TextView view,
                GridLayout layout,
                int columnId){

            layout.addView(view);

            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.height = GridLayout.LayoutParams.WRAP_CONTENT;
            param.width = GridLayout.LayoutParams.WRAP_CONTENT;
            param.rightMargin = 5;
            param.topMargin = 5;
            view.setTextSize(11);
            view.setWidth(TEXT_FIELD_WIDTH[columnId]);
            view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            param.setGravity(Gravity.CENTER);
            param.columnSpec = GridLayout.spec(columnId);
            param.rowSpec = GridLayout.spec(rowId);
            view.setLayoutParams (param);
        }

        @Override
        public void decremntRowId(){
            rowId--;
        }

        @Override
        public int getRowId() {
            return rowId;
        }

        @Override
        public void removeFromGrid(GridLayout grid) {
            grid.removeView(nameView);
            grid.removeView(latView);
            grid.removeView(lonView);
            grid.removeView(altView);
            grid.removeView(clockBiasView);
        }

        @Override
        public void update(CalculationModule calculationModule) {

            //todo: throws error here when executed from incorrect thread?
            //todo: can be causing viewers crash?
            if (nameView != null &&
                    latView != null &&
                    lonView != null &&
                    altView != null &&
                    clockBiasView != null) {

                nameView.setText(calculationModule.getName());
                latView.setText(String.format("%.5f", calculationModule.getPose().getGeodeticLatitude()));
                lonView.setText(String.format("%.5f", calculationModule.getPose().getGeodeticLongitude()));
                altView.setText(String.format("%.1f", calculationModule.getPose().getGeodeticHeight()));
                clockBiasView.setText(String.format("%.0f", calculationModule.getClockBias()));

            }
        }
    }


    private GridLayout poseGridView;
    private GridLayout constellationGridView;
    private HashMap<CalculationModule, CalculationGridItem> poseItems = new HashMap<>();
    private ConstellationGrid constellationGrid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.main_viewer, container, false);

        nameColumnWidth = getResources().getDimension(R.dimen.name_column_width);
        itemColumnWidth = getResources().getDimension(R.dimen.item_column_width);

        poseGridView = rootView.findViewById(R.id.pose_list);
        constellationGridView = rootView.findViewById(R.id.constellation_list);

        if(constellationGrid == null)
            constellationGrid = new ConstellationGrid(constellationGridView);
        else
            constellationGrid.reinitialize(constellationGridView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        redrawGrid(poseGridView, poseItems);
    }

    private void redrawGrid(GridLayout grid, HashMap<CalculationModule, CalculationGridItem> items){

        grid.setRowCount(items.size() + 1);

        for(Map.Entry<CalculationModule, CalculationGridItem> entry: items.entrySet()) {
            entry.getValue().reinitializeViews(grid);
        }
    }

    private void removeSeriesFromGrid(
            CalculationModule calculationModule,
            GridLayout grid,
            Map<CalculationModule, CalculationGridItem> items) {

        int removedRowId = items.get(calculationModule).getRowId();

        for(Map.Entry<CalculationModule, CalculationGridItem> entry : items.entrySet()){
            if(entry.getValue().getRowId()>removedRowId) {
                entry.getValue().decremntRowId();
                entry.getValue().removeFromGrid(grid);
            }
        }

        items.get(calculationModule).removeFromGrid(grid);
        items.remove(calculationModule);

        // opposite order than in redrawGrid
        for(Map.Entry<CalculationModule, CalculationGridItem> entry: items.entrySet()) {
            entry.getValue().reinitializeViews(grid);
        }

        grid.setRowCount(items.size() + 1);
    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {

    }

    private List<CalculationModule> modulesToBeAdded = new ArrayList<>();
    private List<CalculationModule> modulesToBeRemoved = new ArrayList<>();

    @Override
    public void update(CalculationModulesArrayList calculationModules) {

        synchronized (this) {
            modulesToBeAdded.clear();
            modulesToBeRemoved.clear();

            modulesToBeAdded.addAll(Sets.difference(
                    new HashSet<>(calculationModules),
                    poseItems.keySet()));

            modulesToBeRemoved.addAll(
                    Sets.difference(
                            poseItems.keySet(),
                            new HashSet<>(calculationModules)));
        }
    }

    @Override
    public void updateOnUiThread(CalculationModulesArrayList calculationModules) {

        if(constellationGrid==null || poseItems == null)
            return;

        constellationGrid.update(calculationModules);

        for(CalculationModule calculationModule : calculationModules) {
            if (poseItems.containsKey(calculationModule)) {
                try {
                    // update sometimes throws CalledFromWrongThreadException
                    poseItems.get(calculationModule).update(calculationModule);
                } catch (Exception e){
                    Log.e(TAG, "update: Exception thrown" );
                    e.printStackTrace();
                }
            }
        }

        synchronized (this) {

            for (CalculationModule calculationModule : modulesToBeAdded) {
                poseGridView.setRowCount(poseGridView.getRowCount() + 1);
                poseItems.put(calculationModule, new PoseItem(
                        poseGridView,
                        poseGridView.getRowCount() - 1
                ));
                poseItems.get(calculationModule).update(calculationModule);
            }
            modulesToBeAdded.clear();

            for (CalculationModule calculationModule : modulesToBeRemoved) {
                removeSeriesFromGrid(calculationModule, poseGridView, poseItems);
            }
            modulesToBeRemoved.clear();
        }
    }
}
