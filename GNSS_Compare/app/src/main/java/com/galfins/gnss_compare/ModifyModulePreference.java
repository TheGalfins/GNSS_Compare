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

package com.galfins.gnss_compare;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import com.galfins.gnss_compare.Corrections.Correction;

/**
 * Created by Mateusz Krainski on 2/19/2018.
 * This class defines an activity used for modifying settings for created calculation modules
 */
public class ModifyModulePreference extends AppCompatActivity {

    /**
     * Request id for this activity
     */
    private static final int PREFERENCES_REQUEST = 100;

    /**
     * view displaying all the items
     */
    private ListView listView;

    /**
     * Single item on the list. Displays status and settings for a singular calculation module
     */
    public class PreferenceListItem {

        /**
         * Reference to assigned calculation module
         */
        private CalculationModule assignedCalculationModuleReference;

        /**
         *
         * @param assignedCalculationModuleReference calculation module assigned to this item
         */
        public PreferenceListItem(CalculationModule assignedCalculationModuleReference) {
            this.assignedCalculationModuleReference = assignedCalculationModuleReference;
        }

        /**
         *
         * @return name of the assigned calculation module
         */
        public String getName() {
            return assignedCalculationModuleReference.getName();
        }

        /**
         *
         * @return description of the assigned calculation module
         */
        public String getDescription() {
            return assignedCalculationModuleReference.getModuleDescription();
        }

        /**
         *
         * @return reference to the assigned calculation module
         */
        public CalculationModule getCalculationModuleReference() {
            return assignedCalculationModuleReference;
        }

        /**
         * Makes the intent used for starting the module preference activity for the calculation
         * module assigned to this item.
         * @param packageContext context
         * @return intent for starting the activity
         */
        public Intent makeIntent(Context packageContext) {
            Intent intent = new Intent(packageContext, CreateModulePreference.class);

            intent.putExtra(CreateModulePreference.KEY_NAME, assignedCalculationModuleReference.getName());
            intent.putExtra(CreateModulePreference.KEY_CONSTELLATION, assignedCalculationModuleReference.getConstellation().getName());
            intent.putExtra(CreateModulePreference.KEY_PVT_METHOD, assignedCalculationModuleReference.getPvtMethod().getName());
            intent.putExtra(CreateModulePreference.KEY_FILE_LOGGER, assignedCalculationModuleReference.getFileLogger().getName());

            ArrayList<Correction> referencedCorrections = assignedCalculationModuleReference.getCorrections();
            String[] corrections = new String[referencedCorrections.size()];

            for (int i = 0; i < corrections.length; i++)
                corrections[i] = referencedCorrections.get(i).getName();

            intent.putExtra(CreateModulePreference.KEY_CORRECTION_MODULES, corrections);

            return intent;
        }
    }

    /**
     * List of created items
     */
    final private ArrayList<PreferenceListItem> itemsList = new ArrayList<>();

    private View mainView;

    private static GnssCoreService.GnssCoreBinder gnssCoreBinder;
    private static boolean mGnssCoreBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gnssCoreBinder = (GnssCoreService.GnssCoreBinder) service;
            mGnssCoreBound = true;

            for(CalculationModule calculationModule: gnssCoreBinder.getCalculationModules())
                itemsList.add(new PreferenceListItem(calculationModule));

            StableArrayAdapter adapter = new StableArrayAdapter(ModifyModulePreference.this,
                    android.R.layout.simple_list_item_1, itemsList);

            listView.setAdapter(adapter);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculation_preference);

        itemsList.clear();

        listView = findViewById(R.id.listview);

        mainView = findViewById(R.id.main_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mGnssCoreBound)
            bindService(
                    new Intent(this, GnssCoreService.class),
                    mConnection,
                    Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(mConnection);
        mGnssCoreBound = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode >= PREFERENCES_REQUEST && resultCode == RESULT_CANCELED) {
            int itemPosition = requestCode - PREFERENCES_REQUEST;

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            try {
                ((PreferenceListItem) listView.getItemAtPosition(itemPosition))
                        .getCalculationModuleReference()
                        .updateFromDescription(
                                sharedPreferences.getString(CreateModulePreference.KEY_NAME, null),
                                sharedPreferences.getString(CreateModulePreference.KEY_CONSTELLATION, null),
                                sharedPreferences.getStringSet(CreateModulePreference.KEY_CORRECTION_MODULES, null),
                                sharedPreferences.getString(CreateModulePreference.KEY_PVT_METHOD, null),
                                sharedPreferences.getString(CreateModulePreference.KEY_FILE_LOGGER, null)
                        );

            } catch (CalculationModule.NameAlreadyRegisteredException
                    | CalculationModule.NumberOfSeriesExceededLimitException e) {

                Snackbar snackbar = Snackbar
                        .make(mainView, e.getMessage(), Snackbar.LENGTH_LONG);

                snackbar.show();
            }

            recreate();
        }
    }

    /**
     * Adapter binding the listView with the list items
     */
    private class StableArrayAdapter extends ArrayAdapter<PreferenceListItem> {

        /**
         * @param context context
         * @param textViewResourceId The resource ID for a layout file containing a TextView to use when instantiating views.
         * @param objects The objects to represent in the ListView.
         */
        public StableArrayAdapter(Context context, int textViewResourceId,
                                  ArrayList<PreferenceListItem> objects) {
            super(context, textViewResourceId, objects);
        }

        /**
         * Defines the name TextView
         * @param v used view
         * @param pos item pos
         */
        private void defineNameView(View v, final int pos){
            final TextView textViewName = v.findViewById(R.id.list_content);
            textViewName.setText(getItem(pos).getName());
        }

        /**
         * Defines the description TextView
         * @param v used view
         * @param pos item pos
         */
        private void defineDescriptionView(View v, final int pos){
            final TextView textVIewDescription = v.findViewById(R.id.description);
            textVIewDescription.setText(getItem(pos).getDescription());
        }

        /**
         * Defines the active Switch
         * @param v used view
         * @param pos item pos
         */
        private void defineActiveSwitch(View v, final int pos){
            Switch switchReference = v.findViewById(R.id.on_switch);
            switchReference.setChecked(getItem(pos).getCalculationModuleReference().getActive());

            switchReference.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getItem(pos).getCalculationModuleReference().setActive(isChecked);

                    String notificationText;
                    if (isChecked)
                        notificationText = "Calculations activated";
                    else
                        notificationText = "Calculations deactivated";

                    Snackbar snackbar = Snackbar
                            .make(mainView, notificationText, Snackbar.LENGTH_SHORT);

                    snackbar.show();
                }
            });
        }

        /**
         * Defines the activate logging Switch
         * @param v used view
         * @param pos item pos
         */
        private void defineLogSwitch(View v, final int pos){
            Switch switchReference = v.findViewById(R.id.save_to_file_switch);
            switchReference.setChecked(getItem(pos).getCalculationModuleReference().isLogToFile());

            switchReference.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getItem(pos).getCalculationModuleReference().setLogToFile(isChecked);

                    String notificationText;
                    if (isChecked)
                        notificationText = "Logging results to file";
                    else
                        notificationText = "Logging results to file terminated";

                    Snackbar snackbar = Snackbar
                            .make(mainView, notificationText, Snackbar.LENGTH_SHORT);

                    snackbar.show();
                }
            });
        }

        /**
         * Defines the button which deletes created calculation module
         * @param v used view
         * @param pos item pos
         */
        private void defineDeleteButton(View v, final int pos){
            Button buttonReference = v.findViewById(R.id.delete_button);

            buttonReference.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(mGnssCoreBound)
                        gnssCoreBinder.removeModule(getItem(pos).getCalculationModuleReference());

                    StableArrayAdapter.this.remove(getItem(pos));
                    StableArrayAdapter.this.notifyDataSetChanged();

                    recreate();
                }
            });
        }

        /**
         * Defines the button which is used to modify the calculation module
         * @param v used view
         * @param pos item pos
         */
        private void defineModifyButton(View v, final int pos){
            Button buttonReference = v.findViewById(R.id.modify_button);

            buttonReference.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final PreferenceListItem item = getItem(pos);

                    Intent intent = item.makeIntent(ModifyModulePreference.this);

                    startActivityForResult(intent, PREFERENCES_REQUEST + pos);
                }
            });
        }

        @NonNull
        @Override
        public View getView(final int pos, View convertView, @NonNull ViewGroup parent) {

            // todo: use the View Holder pattern
            View v;// = convertView;
//            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.calculation_preference_item, null);

                try {

                    defineNameView(v, pos);
                    defineDescriptionView(v, pos);
                    defineActiveSwitch(v, pos);
                    defineLogSwitch(v, pos);
                    defineDeleteButton(v, pos);
                    defineModifyButton(v, pos);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
//            }
            return v;
        }

    }

}
