
*******************
Example of analysis
*******************

This section provides information about the scenarios in which *GNSS Compare* was tested and the PVT performance
obtained from its estimation algorithms (e.g., Extended Kalman Filter). Furthermore, the analysis
presented here serves also as an example of how GNSS Compare can be used for algorithmic performance assessment.
For a preliminary PVT performance assessment the following scenarios were considered: **Static user**, **Pedestrian user** and **Dynamic user**.

With this section we would like to give you an idea of how *GNSS Compare* can be used. The application allows
data logging (e.g., results of the PVT estimations) in different formats, like NMEA and a custom one. These files
can be retrieved from the phone and then processed in your favourit programming environment for analysis. More details about the
logging formats of *GNSS Compare* will be given soon.

*Note 1: Please be aware that the results presented here are specific to the environment/time when they were generated and they cannot be interpreted in a general sense.*

*Note 2: The Extended Kalman Filters were initialized with the Android FINE location.*

Static user
================

Let's take a look at some details about this scenario:

- Reference location: Latitude 52.16954469, Longitude 4.48089101, Altitude 55.48 m
- Data collection duration: approximately 4 minutes
- Enabled constellations: Galileo, GPS, Galileo+GPS
- Number of used satellites: 4 Galileo and 5 GPS

After the results of the PVT estimations were obtained from the logged files of *GNSS Compare*, they were projected
in Google Earth as seen in the figure below for an initial analysis.

.. image:: imgAnalysis/StaticgoogleEarth.PNG
    :width: 60%
    :align: center
    :alt: TheGalfins


Pedestrian user
===============


Dynamic user
============
