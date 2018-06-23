
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
the definition of each used variable (e.g., *ReceivedSvTimeNanos* ) can be found on the `Android Developer`_ webpage or in the white paper mentioned above. We will keep things
straight forward in this section.

Galileo
=======

Roughly speaking, the pseudorange is the difference between the time of signal reception and the time of signal transmission multiplied by they speed of light. Therefore, let's see how we compute the time of signal reception with the Android raw parameters:

.. code-block:: java

    galileoTime = TimeNanos - (FullBiasNanos + BiasNanos)
    tRxGalileoTOW = galileoTime % Constants.NUMBER_NANO_SECONDS_PER_WEEK
    tRxGalileoE1_2nd = galileoTime % Constants.NumberNanoSeconds100Milli

It may look a bit strange that we compute two times of reception ( *tRxGalileoTOW* and *tRxGalileoE1_2nd* ) however there is an
explanation behind this.

GPS
====








.. _`White Paper on using GNSS Raw Measurements on Android devices`: https://www.gsa.europa.eu/newsroom/news/available-now-white-paper-using-gnss-raw-measurements-android-devices
.. _`Android Developer`: https://developer.android.com/reference/android/location/GnssMeasurement
