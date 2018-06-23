
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
the definition of each used variable (e.g., *ReceivedSvTimeNanos*) can be found on the `Android Developer`_ webpage or in the white paper mentioned above. We will keep things
straight forward in this section.

A final note, the pseduroanges computed here are based on the ranging codes that modulate the L1 carrier signal.


Galileo
=======

Roughly speaking, the pseudorange is the difference between the time of signal reception and the time of signal transmission multiplied by the speed of light. Therefore, let's see how we compute the time of signal reception with the Android raw parameters:

.. code-block:: java

    galileoTime = TimeNanos - (FullBiasNanos + BiasNanos);
    tRxGalileoTOW = galileoTime % Constants.NUMBER_NANO_SECONDS_PER_WEEK;
    tRxGalileoE1_2nd = galileoTime % Constants.NumberNanoSeconds100Milli;

It may look a bit strange that we compute two times of reception ( *tRxGalileoTOW* and *tRxGalileoE1_2nd*) however there is reason behind this. We have to be aware of the fact that Galileo signals have more complex modulation schemes if compared with the legacy signals of GPS. In this sense, processing Galileo signals requires more effort from the GNSS receiver. Now in order to use the Galileo pseudoranges in the PVT estimation, these pseudoranges have to pass some kind of health check. One of these checks looks if the Time Of Week (TOW) parameter is decoded or determined from other sources (e.g., mobile network), and the other one checks if the smartphone's GNSS receiver is locked on the Galileo E1 secondary code. We will see soon how this is handled. However, we will not deal with the theoretical background in order to reason the approach presented here because it
does require some advanced receiver signal processing knowledge and at this point this is outside of our aims. In exchange, we can advise the curious minds to check a book on GNSS signal structures, like *Engineering Satellite-Based Navigation and Timing: Global Navigation Satellite Systems, Signals and Receivers by John W. Betz*.

Therefore we will use *tRxGalileoTOW* and *tRxGalileoE1_2nd* to compute two pseudoranges and we will use only one them, the one that manages to pass the health check of course! Now let's compute the time of signal transmission:

.. code-block:: java

   tTxGalileo = ReceivedSvTimeNanos + TimeOffsetNanos;

The two pseudoranges are:

.. code-block:: java

   pseudorangeTOW = (tRxGalileoTOW - tTxGalileo) * 1e-9 * Constants.SPEED_OF_LIGHT;
   pseudorangeE1_2nd = ((galileoTime - tTxGalileo) % Constants.NumberNanoSeconds100Milli) * 1e-9 * Constants.SPEED_OF_LIGHT;

We have said that we need to test these two pseudoranges for some criteria. And the Java object containing the *health status* or
the *states* that we wish to find if they are true or not is:

.. code-block:: java

   int measState = measurement.getState();

With the help of the bitwise AND operation we can identify if the seeked states are true or not. Please check the `Android Developer`_ website to have a better insight of this process:

.. code-block:: java

    boolean towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) > 0;
    boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;
    boolean codeLock = (measState & GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK) > 0;

Finally, we do the following check and we decide which computed pseudorange we use:

.. code-block:: java

    if ((towKnown || towDecoded)) {

        // use pseudorangeTOW

    }else if (codeLock){

       // use pseudorangeE1_2nd

    }

GPS
====

We follow a similar approach for GPS also by starting to compute the time of signal reception:

.. code-block:: java

       gpsTime = TimeNanos - (FullBiasNanos + BiasNanos);
       tRxGPS  = gpsTime + TimeOffsetNanos;

In the next step we compute in a more straight forward way the GPS pseudorange:

.. code-block:: java

       weekNumberNanos = Math.floor((-1. * FullBiasNanos) / Constants.NUMBER_NANO_SECONDS_PER_WEEK)*onstants.NUMBER_NANO_SECONDS_PER_WEEK;
       pseudorange = (tRxGPS - weekNumberNanos - ReceivedSvTimeNanos) / 1.0E9 * Constants.SPEED_OF_LIGHT;




.. _`White Paper on using GNSS Raw Measurements on Android devices`: https://www.gsa.europa.eu/newsroom/news/available-now-white-paper-using-gnss-raw-measurements-android-devices
.. _`Android Developer`: https://developer.android.com/reference/android/location/GnssMeasurement
