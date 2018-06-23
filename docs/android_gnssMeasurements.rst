
******************************
Android GNSS raw measurements
******************************

In the following sections we describe the algorithms used to compute the pseudoranges
taken into account the used satellite navigation system. The following algorithms are based on
the European GNSS Agency's (GSA) `White Paper on using GNSS Raw Measurements on Android devices`_.

At the code level, you can find the algorithms in the following Java classes:

*GalileoConstellation*

*GpsConstellation*

The variable names used in the description of the algorithms are the same as the ones in the GNSS Compare's code. Moreover,
the definition of each used variable (e.g., ReceivedSvTimeNanos) can be found on the `Android Developer`_ webpage. We will keep things
straight forward in this section.

Galileo
=======




GPS
====








.. _`White Paper on using GNSS Raw Measurements on Android devices`: https://www.gsa.europa.eu/newsroom/news/available-now-white-paper-using-gnss-raw-measurements-android-devices
.. _`Android Developer`: https://developer.android.com/reference/android/location/GnssMeasurement
