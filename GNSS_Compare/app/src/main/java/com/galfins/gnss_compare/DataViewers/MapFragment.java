package com.galfins.gnss_compare.DataViewers;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.galfins.gnss_compare.CalculationModule;
import com.galfins.gnss_compare.CalculationModulesArrayList;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gnss_compare.R;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by Mateusz Krainski on 31/03/2018.
 * This class is for...
 */

public class MapFragment extends Fragment implements DataViewer, OnMapReadyCallback {

    MapView mapView;
    GoogleMap map;
    LatLng mapCameraLocation = new LatLng(0.0, 0.0);
    boolean mapCameraLocationInitialized = false;
    float mapCameraZoom = 12;

    Observable uiThreadObservableReference;

    CameraUpdate mapCameraUpdateAnimation;

    Map<CalculationModule, MapDataSeries> dataSeries = new HashMap<>();

    private boolean mapInActivity = false;

    private class MapDataSeries implements LocationSource {

        private static final String TAG = "MapDataSeries";
        private final int MAX_PLOTTED_POINTS;
        ArrayList<SafeMarkerDescription> registeredMarkerOptions;
        ArrayList<Marker> registeredMarkers;
        private Observer observer;

        private OnLocationChangedListener mListener;

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mListener = onLocationChangedListener;
        }

        @Override
        public void deactivate() {
            mListener = null;
        }

        private class SafeMarkerDescription {
            private Coordinates location;
            private int drawableReference;
            private int color;

            SafeMarkerDescription(Coordinates location, int drawableReference, int color) {
                this.location = location;
                this.drawableReference = drawableReference;
                this.color = color;
            }

            MarkerOptions getMarkerOptions() {
                return new MarkerOptions()
                        .position(new LatLng(
                                location.getGeodeticLatitude(),
                                location.getGeodeticLongitude()))
                        .icon(vectorToBitmap(
                                drawableReference,
                                color));
            }
        }

        MapDataSeries(int plottedPoints) {
            MAX_PLOTTED_POINTS = plottedPoints;
//            calculationModuleReference = calculationModule;
            registeredMarkerOptions = new ArrayList<>();
            registeredMarkers = new ArrayList<>();

//            observer = new Observer() {
//                @Override
//                public void update(Observable observable, Object o) {
//                    if (o != calculationModuleReference)
//                        return;
//
//                    Coordinates currentPose = calculationModuleReference.getPose();
//
//                    if (map != null) {
//                        registeredMarkerOptions.add(
//                                new SafeMarkerDescription(
//                                        currentPose,
//                                        R.drawable.map_dot_black_24dp,
//                                        calculationModuleReference.getDataColor()));
//
//                        SafeMarkerDescription lastMarker =
//                                registeredMarkerOptions.get(registeredMarkerOptions.size() - 1);
//
//                        if (mapInActivity) {
//                            registeredMarkers.add(
//                                    map.addMarker(lastMarker.getMarkerOptions()));
//                        }
//
//                        if (registeredMarkerOptions.size() > MAX_PLOTTED_POINTS) {
//                            registeredMarkerOptions.remove(0);
//                            if (registeredMarkers.size() > 0) {
//                                registeredMarkers.get(0).remove();
//                                registeredMarkers.remove(0);
//                            }
//                        }
//
//                        if (mListener != null && mapInActivity) {
//                            mListener.onLocationChanged(calculationModuleReference.getLocationFromGoogleServices());
//                        }
//                    } else {
//                        Log.w(TAG, "update: Map not yet initialized...");
//                    }
//
//                }
//            };
        }

        public void update(CalculationModule calculationModule){
            Coordinates currentPose = calculationModule.getPose();

            if (map != null) {
                registeredMarkerOptions.add(
                        new SafeMarkerDescription(
                                currentPose,
                                R.drawable.map_dot_black_24dp,
                                calculationModule.getDataColor()));

                SafeMarkerDescription lastMarker =
                        registeredMarkerOptions.get(registeredMarkerOptions.size() - 1);

                if (mapInActivity) {
                    registeredMarkers.add(
                            map.addMarker(lastMarker.getMarkerOptions()));
                }

                if (registeredMarkerOptions.size() > MAX_PLOTTED_POINTS) {
                    registeredMarkerOptions.remove(0);
                    if (registeredMarkers.size() > 0) {
                        registeredMarkers.get(0).remove();
                        registeredMarkers.remove(0);
                    }
                }

                if (mListener != null && mapInActivity) {
                    mListener.onLocationChanged(calculationModule.getLocationFromGoogleServices());
                }
            } else {
                Log.w(TAG, "update: Map not yet initialized...");
            }
        }

        public Observer getDataObserver() {
            return observer;
        }

        public void resetMarkers() {
            registeredMarkers.clear();
            for (SafeMarkerDescription markerDescription : registeredMarkerOptions) {
                registeredMarkers.add(map.addMarker(markerDescription.getMarkerOptions()));
            }
        }

        public ArrayList<Marker> getMarkers() {
            return registeredMarkers;
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ViewGroup rootView;

        if(MainActivity.getMetaDataString("com.google.android.geo.API_KEY").equals("YOUR_API_KEY")){

            rootView = (ViewGroup) inflater.inflate(
                    R.layout.map_disabled_layout, container, false);

            TextView t2 = rootView.findViewById(R.id.description);
            t2.setMovementMethod(LinkMovementMethod.getInstance());

        } else {

            rootView = (ViewGroup) inflater.inflate(
                    R.layout.map_page, container, false);

            mapView = rootView.findViewById(R.id.map);
            mapView.onCreate(null);

            mapView.getMapAsync(this);
            mapInActivity = true;
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(map != null) {
            if(mapCameraLocationInitialized) {
                mapCameraLocation = map.getCameraPosition().target;
                mapCameraZoom = map.getCameraPosition().zoom;
            }
            map.clear();
        }

        mapInActivity = false;

    }

    @Override
    public void addSeries(CalculationModule calculationModule) {
        dataSeries.put(calculationModule, new MapDataSeries(10));
        addLocationSource(dataSeries.get(calculationModule));
//        uiThreadObservableReference.addObserver(getSeries(calculationModule).getDataObserver());
//        calculationModule.addObserver(getSeries(calculationModule).getDataObserver());
    }

    /**
     * Demonstrates converting a {@link Drawable} to a {@link BitmapDescriptor},
     * for use as a marker icon.
     */
    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Removes the series associated with {@code calculationModule} from the plot
     * @param calculationModule reference to the calculation module
     */
    @Override
    public void removeSeries(CalculationModule calculationModule){
        if (map != null) {

            MapDataSeries reference = dataSeries.get(calculationModule);
            for (Marker marker : reference.getMarkers())
                marker.remove();

            dataSeries.remove(calculationModule);

        }
    }

    @Override
    public void onLocationFromGoogleServicesResult(Location location) {
        if(!mapCameraLocationInitialized) {

            mapCameraLocation = new LatLng(location.getLatitude(), location.getLongitude());

            mapCameraLocationInitialized = true;

            if(map!=null){
                mapCameraUpdateAnimation = CameraUpdateFactory.newLatLngZoom(
                        mapCameraLocation, mapCameraZoom);
                map.moveCamera(mapCameraUpdateAnimation);
            }
        }

    }

    @Override
    public void registerToUiThreadedUpdates(Observable uiTheadObservable) {
//        for(MapDataSeries series : dataSeries)
//            uiTheadObservable.addObserver(series.getDataObserver());
//
//        uiThreadObservableReference = uiTheadObservable;
    }

    @Override
    public void update(CalculationModulesArrayList calculationModules) {

    }

    Set<CalculationModule> seenModules = new HashSet<>();
    Set<CalculationModule> calculationModulesSet;

    @Override
    public void updateOnUiThread(CalculationModulesArrayList calculationModules) {

        calculationModulesSet = new HashSet<>(calculationModules);

        // modules to be added
        for (CalculationModule calculationModule : Sets.difference(
                calculationModulesSet,
                seenModules)) {
            addSeries(calculationModule);
            seenModules.add(calculationModule);
        }

        // modules to be removed
        for (CalculationModule calculationModule : Sets.difference(
                seenModules,
                calculationModulesSet)) {
            removeSeries(calculationModule);
            seenModules.remove(calculationModule);
        }

        for(CalculationModule calculationModule: calculationModules)
            dataSeries.get(calculationModule).update(calculationModule);

    }

    private void addLocationSource(MapDataSeries locationSourceToBeAdded){
        if (locationSourceToBeAdded != null && map!=null) {
            map.setLocationSource(locationSourceToBeAdded);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {

        this.map = map;

        mapCameraUpdateAnimation = CameraUpdateFactory.newLatLngZoom(
                mapCameraLocation, mapCameraZoom);

        map.moveCamera(mapCameraUpdateAnimation);

        for(Map.Entry<CalculationModule, MapDataSeries> entry: dataSeries.entrySet()){
            entry.getValue().resetMarkers();
            addLocationSource(entry.getValue());
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if(mapView != null)
            mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mapView != null)
            mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mapView != null)
            mapView.onStop();
    }


    @Override
    public void onPause() {
        if(mapView != null)
            mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(mapView != null)
            mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if(mapView != null)
            mapView.onLowMemory();
    }
}
