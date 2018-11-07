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

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.FileLoggers.FileLogger;
import com.galfins.gnss_compare.PvtMethods.PvtMethod;

/**
 * Created by Mateusz Krainski on 18/1/2018.
 * This class defines an activity used for creating a new calculation module
 *
 */

public class CreateModulePreference extends PreferenceActivity {

    /**
     * Key for setting storing constellation name
     */
    static final public String KEY_CONSTELLATION="constellation";

    /**
     * Key for setting storing correction module names
     */
    static final public String KEY_CORRECTION_MODULES="correction_module";

    /**
     * Key for setting storing pvt method name
     */
    static final public String KEY_PVT_METHOD="pvt_method";

    /**
     * Key for setting storing file logger name
     */
    static final public String KEY_FILE_LOGGER="file_logger";

    /**
     * Key for setting storing created module name
     */
    static final public String KEY_NAME="name";

    /**
     * Counter storing how many modules have already been created. Used for autogeneration of names
     */
    static private int invocationCounter=0;

    /**
     * A reference to an alphabet. Used for autogeneration of unique names
     */
    static private final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * Increments the invocationCounter after the module has been successfully created.
     */
    static void notifyModuleCreated(){
        if(++invocationCounter > ALPHABET.length)
            invocationCounter = 0;
    }

    /**
     * Tag for console log purposes.
     */
    static private final String TAG="CreateModulePreference";

    /**
     * Observer notified when the preference fragment is finished.
     */
    private Observer finishSignalObserver = new Observer() {
        @Override
        public void update(Observable observable, Object o) {
            setResult(RESULT_OK);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CreateModulePreferenceFragment fragment = new CreateModulePreferenceFragment();
        fragment.setArguments(getIntent().getExtras());
        fragment.assignObserver(finishSignalObserver);

        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
        setResult(RESULT_CANCELED);
    }

    /**
     * Fragment used for the preferences.
     */
    static public class CreateModulePreferenceFragment extends PreferenceFragment
    {
        /**
         * Tag used for console logging
         */
        static final private String TAG="CreateModulePreferenceFragment";

        /**
         * Observable, which notifies observers that the fragment has finished
         */
        Observable finishSignal  = new Observable(){
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers(); }
        };

        /**
         * Assigns observer, which will be notified about the fragment completion
         * @param assignedObserver observer to be assigned
         */
        public void assignObserver(Observer assignedObserver){
            finishSignal.addObserver(assignedObserver);
        }

        /**
         * Assigns a click listener to the preference button.
         * @param intentExtras extras passed to the fragment
         */
        private void defineCreateButton(Bundle intentExtras){
            if(intentExtras == null) {
                Preference button = findPreference(getString(R.string.create_module_button));
                button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        finishSignal.notifyObservers();
                        return true;
                    }
                });
            }
        }

        /**
         * defines a list preference
         * @param preferenceKey Key of the preference
         * @param registeredObjectNames set containing possible values in the list preference
         * @param intentExtras intent passed to the fragment
         */
        private void defineListPrefernece(String preferenceKey, Set<String> registeredObjectNames, Bundle intentExtras) {

            ListPreference preference = (ListPreference) findPreference(preferenceKey);
            CharSequence[] entries =
                    registeredObjectNames.toArray(new CharSequence[registeredObjectNames.size()]);

            preference.setEntries(entries);
            preference.setEntryValues(entries);

            if(intentExtras != null){
                preference.setValue(intentExtras.getString(preferenceKey));
            } else {
                preference.setValue(null);
            }
        }

        /**
         * defines a multiSelectList preference
         * @param preferenceKey Key of the preference
         * @param registeredObjectNames set containing possible values in the list preference
         * @param intentExtras intent passed to the fragment
         */
        private void defineMultiSelectListPrefernece(String preferenceKey, Set<String> registeredObjectNames, Bundle intentExtras) {

            MultiSelectListPreference preference = (MultiSelectListPreference) findPreference(preferenceKey);
            CharSequence[] entries =
                    registeredObjectNames.toArray(new CharSequence[registeredObjectNames.size()]);

            preference.setEntries(entries);
            preference.setEntryValues(entries);

            if(intentExtras != null){
                if(intentExtras.getStringArray(preferenceKey) == null){
                    preference.setValues(null);
                } else {
                    Set<String> selectedCorrections = new HashSet<>(Arrays.asList(intentExtras.getStringArray(preferenceKey)));
                    preference.setValues(selectedCorrections);
                }
            } else {
                preference.setValues(new HashSet<String>());
            }
        }

        /**
         * defines a name preference
         * @param intentExtras intent passed to the preference
         */
        private void defineNamePreference(Bundle intentExtras){
            EditTextPreference prefName =
                    (EditTextPreference) findPreference(KEY_NAME);

            if(intentExtras == null) {
                prefName.setText("Scheme " + ALPHABET[invocationCounter]);
            } else {
                prefName.setText(intentExtras.getString(KEY_NAME));
            }
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            Bundle intentExtras = getArguments();

            if(intentExtras == null)
                addPreferencesFromResource(R.xml.preferences);
            else
                addPreferencesFromResource(R.xml.preferences_modify);

            defineListPrefernece(KEY_CONSTELLATION,
                    Constellation.getRegistered(),
                    intentExtras);

            defineMultiSelectListPrefernece(KEY_CORRECTION_MODULES,
                    Correction.getRegistered(),
                    intentExtras);

            defineListPrefernece(KEY_PVT_METHOD,
                    PvtMethod.getRegistered(),
                    intentExtras);

            defineListPrefernece(KEY_FILE_LOGGER,
                    FileLogger.getRegistered(),
                    intentExtras);

            defineNamePreference(intentExtras);

            defineCreateButton(intentExtras);
        }
    }
}


