package com.galfins.gnss_compare;

import org.junit.Test;

import com.galfins.gnss_compare.GoGpsExtracts.Time;
import com.galfins.gnss_compare.GoGpsExtracts.Coordinates;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {

        // Parameters obtained from the Smartphone's GPS receiver
        int gpsWeek = 1986;
        int gpsSow = 58711;
        Time currentTime = new Time(gpsWeek,gpsSow);

        // Declare receiver's approximate coordinates
        double X = 3900473.8289;
        double Y = 5021740.2920;
        double Z = 499667.6269;
        Coordinates approximatedPose = Coordinates.globalXYZInstance(X, Y, Z);




    }

    @Test
    public void testCalculations() throws Exception{

    }
}