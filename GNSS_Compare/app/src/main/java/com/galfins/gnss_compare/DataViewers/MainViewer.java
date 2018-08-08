package com.galfins.gnss_compare.DataViewers;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gnss_compare.R;

/**
 * Created by Mateusz Krainski on 04/05/2018.
 * This is the main data viewer, which displays data in a text form.
 */
public class MainViewer extends Fragment implements DataViewer {

    double nameColumnWidth = 0.0;
    double itemColumnWidth = 0.0;

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
         *
         * @return assigned calculation module
         */
        CalculationModule getCalculationModuleReference();

        /**
         * decrements row number on which this item is stored. Used when another item (above current one)
         * is removed from the grid layout
         */
        void decremntRowId();

        /**
         * Detaches the observer associated with updating data
         */
        void detachObserver();

        /**
         * Removes item from grid
         * @param grid layout to which the item was assigned
         */
        void removeFromGrid(GridLayout grid);
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
        List<Constellation> listOfRegisteredConstellations = new ArrayList<>();

        public ConstellationGrid(GridLayout layout){
            referenceToLayout = layout;

            for(String key : Constellation.getRegistered()){
                try {
                    Constellation constellation = Constellation.getClassByName(key).newInstance();
                    listOfRegisteredConstellations.add(constellation);
                    referenceToLayout.setRowCount(referenceToLayout.getRowCount()+1);
                    items.put(constellation.getName(), new ConstellationItem(referenceToLayout, items.size()+1));
                } catch (java.lang.InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        public void reinitialize(GridLayout constellationGridView) {
            for(Map.Entry<String, ConstellationItem> item : items.entrySet())
                item.getValue().removeFromGrid(referenceToLayout);

            items.clear();

            referenceToLayout = constellationGridView;
            referenceToLayout.setRowCount(listOfRegisteredConstellations.size()+1);

            for(Constellation constellation : listOfRegisteredConstellations)
                items.put(constellation.getName(), new ConstellationItem(referenceToLayout, items.size()+1));
        }

        private Observer calculationUpdatedObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                items.get(((CalculationModule.CalculationModuleObservable) o).getParentReference().getConstellation().getName())
                    .updateViews(((CalculationModule.CalculationModuleObservable) o).getParentReference().getConstellation());
            }
        };

        public Observer getCalculationUpdatedObserver() {
            return calculationUpdatedObserver;
        }

        public void addSeries(CalculationModule calculationModule){
            calculationModule.addObserver(calculationUpdatedObserver);
        }

        public void removeSeries(CalculationModule calculationModule){
            items.get(calculationModule.getConstellation().getName()).initializeViewsAsEmpty();
            calculationModule.removeObserver(calculationUpdatedObserver);
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
        private CalculationModule calculationModuleReference;

        private TextView nameView;
        private TextView latView;
        private TextView lonView;
        private TextView altView;
        private TextView clockBiasView;

        private int rowId;

        private Observer updater = new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                updateViews();
            }
        };

        public PoseItem(CalculationModule calculationModule, GridLayout gridLayout, int rowId){
            calculationModuleReference = calculationModule;

            this.rowId = rowId;

            reinitializeViews(gridLayout);

            calculationModuleReference.addObserver(updater);
        }

        @Override
        public CalculationModule getCalculationModuleReference() {
            return calculationModuleReference;
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

                updateViews();
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

        private void updateViews(){
            if(nameView != null &&
                    latView != null &&
                    lonView != null &&
                    altView != null &&
                    clockBiasView != null) {

                nameView.setText(calculationModuleReference.getName());
                latView.setText(String.format("%.5f", calculationModuleReference.getPose().getGeodeticLatitude()));
                lonView.setText(String.format("%.5f", calculationModuleReference.getPose().getGeodeticLongitude()));
                altView.setText(String.format("%.1f", calculationModuleReference.getPose().getGeodeticHeight()));
                clockBiasView.setText(String.format("%.0f", calculationModuleReference.getClockBias()));
            }
        }

        @Override
        public void decremntRowId(){
            rowId--;
        }

        @Override
        public void detachObserver(){
            calculationModuleReference.removeObserver(updater);
        }

        @Override
        public void removeFromGrid(GridLayout grid) {
            grid.removeView(nameView);
            grid.removeView(latView);
            grid.removeView(lonView);
            grid.removeView(altView);
            grid.removeView(clockBiasView);
        }
    }


    private GridLayout poseGridView;
    private GridLayout constellationGridView;
    private List<CalculationGridItem> poseItems = new ArrayList<>();
    private Button logButton;
    private ConstellationGrid constellationGrid;

    private static List<CalculationModule> seriesAddedBeforeInitialization = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.main_viewer, container, false);

        nameColumnWidth = getResources().getDimension(R.dimen.name_column_width);
        itemColumnWidth = getResources().getDimension(R.dimen.item_column_width);

        poseGridView = rootView.findViewById(R.id.pose_list);
        constellationGridView = rootView.findViewById(R.id.constellation_list);
        logButton = rootView.findViewById(R.id.log_button);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!MainActivity.rawMeasurementsLogger.isStarted()) {
                    MainActivity.rawMeasurementsLogger.startNewLog();
                    logButton.setText(R.string.stop_log_button_description);
                } else {
                    MainActivity.rawMeasurementsLogger.closeLog();
                    logButton.setText(R.string.start_log_button_description);
                }
            }
        });

        if(constellationGrid == null)
            constellationGrid = new ConstellationGrid(constellationGridView);
        else
            constellationGrid.reinitialize(constellationGridView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(poseGridView != null) {
            for (CalculationModule calculationModule : seriesAddedBeforeInitialization)
                addSeries(calculationModule);
            seriesAddedBeforeInitialization.clear();
        }

        redrawGrid(poseGridView, poseItems);
    }

    private void redrawGrid(GridLayout grid, List<CalculationGridItem> items){

        grid.setRowCount(items.size() + 1);

        for(CalculationGridItem item: items) {
            item.reinitializeViews(grid);
        }
    }

    @Override
    public void addSeries(CalculationModule calculationModule) {

        if(poseGridView==null || constellationGrid==null){
            seriesAddedBeforeInitialization.add(calculationModule);
        } else {

            poseGridView.setRowCount(poseGridView.getRowCount() + 1);

            poseItems.add(new PoseItem(
                    calculationModule,
                    poseGridView,
                    poseGridView.getRowCount() - 1
            ));

            constellationGrid.addSeries(calculationModule);
        }
    }

    private void removeSeriesFromGrid(
            CalculationModule calculationModule,
            GridLayout grid,
            List<CalculationGridItem> items) {

        boolean itemFound = false;
        for(int i=0; i<items.size(); i++){
            if(items.get(i).getCalculationModuleReference() == calculationModule){
                items.get(i).detachObserver();
                items.get(i).removeFromGrid(grid);
                items.remove(i);

                itemFound = true;
            }
            if(itemFound && i<items.size()) {
                items.get(i).decremntRowId();
                items.get(i).removeFromGrid(grid);
            }
        }

        // opposite order than in redrawGrid
        for(CalculationGridItem item: items) {
            item.reinitializeViews(grid);
        }

        grid.setRowCount(items.size() + 1);
    }

    @Override
    public void removeSeries(CalculationModule calculationModule) {
        if(poseGridView == null)
            seriesAddedBeforeInitialization.remove(calculationModule);
        else
            removeSeriesFromGrid(calculationModule, poseGridView, poseItems);

        constellationGrid.removeSeries(calculationModule);
    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {

    }
}
