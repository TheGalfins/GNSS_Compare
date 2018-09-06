package com.galfins.gnss_compare.DataViewers;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Observable;

/**
 * A simple pager adapter that represents DataViewer objects, in
 * sequence.
 */
public class DataViewerAdapter extends FragmentStatePagerAdapter {

    /**
     * ArrayList storing the viewers. This is of type Object
     * because the abstract type needs to both implement DataViewer and extend Fragment.
     */
    private ArrayList<Object> registeredViewers;

    /**
     *
     * @param fm connected fragment manager
     */
    public DataViewerAdapter(FragmentManager fm) {
        super(fm);
        registeredViewers = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int position) {
        return (Fragment) registeredViewers.get(position);
    }

    @Override
    public int getCount() {
        return registeredViewers.size();
    }

    /**
     * Adds a new fragment to the list of available fragments
     * @param newFragment new fragment to be registered
     */
    public void registerFragment(Fragment newFragment) {
        registeredViewers.add(newFragment);
    }

    /**
     * Returns registered viewers as a DataViewer ArrayList
     * @return
     */
    public ArrayList<DataViewer> getViewers() {
        ArrayList<DataViewer> newArray = new ArrayList<>();
        for (Object viewer : registeredViewers) {
            newArray.add((DataViewer) viewer);
        }
        return newArray;
    }

    /**
     * Creates all used data viewers
     */
    public void initialize(){
        this.registerFragment(new MainViewer());
//        this.registerFragment(new PowerPlotFragment());
//        this.registerFragment(new PoseErrorFragment());
//        this.registerFragment(new MapFragment());
    }

    public void registerUiThreadObservable(Observable observable){
        for(Object viewer : registeredViewers){
            ((DataViewer) viewer).registerToUiThreadedUpdates(observable);
        }
    }


}
