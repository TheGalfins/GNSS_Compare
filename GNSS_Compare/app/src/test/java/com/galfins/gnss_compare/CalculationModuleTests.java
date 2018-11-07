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

import android.util.ArraySet;

import org.junit.Test;

import static org.junit.Assert.*;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.FileLoggers.FileLogger;
import com.galfins.gnss_compare.PvtMethods.PvtMethod;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class CalculationModuleTests {

    /**
     * Testing creation from description
     * @throws Exception
     */
    @Test
    public void CreateByNameTest() throws Exception {

        CalculationModule.clear();

        Constellation.initialize(false);
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();

        String moduleName = "Test";
        String constellationName = "GPS";
        String correctionName = "Klobuchar Iono Correction";
        Set<String> correctionNames = new HashSet<>();
        correctionNames.add(correctionName);
        String pvtMethodName = "Weighted Least Squares";
        String fileLoggerName = "SimpleFormat";

        CalculationModule calculationModule = CalculationModule.createFromDescriptions(
                moduleName,
                constellationName,
                correctionNames,
                pvtMethodName,
                fileLoggerName
        );


        assertEquals("Module name differs", calculationModule.getName(), moduleName);

        assertEquals("Constellation name differs!", calculationModule.getConstellation().getName(), constellationName);

        assertEquals("Correction differs!", calculationModule.getCorrections().get(0).getName(), correctionName);

        assertEquals("PVT method differs!", calculationModule.getPvtMethod().getName(), pvtMethodName);

        assertEquals("File logger differs!", calculationModule.getFileLogger().getName(), fileLoggerName);

    }
}