
***************************************
Getting started with the User Interface
***************************************

Here we show what you can see at the User Interface (UI) level and we also
describe how to set up different processing schemes, effect of which can be studied in
real-time.

Application's Views
===================

We refer to a *view* as the current content displayed by the application. The user
can change these views by swiping on the phone's screen. Curently we have the following
views: **Main View**, **Satellite signal stregth**, **Positioning error plot** and **Google Maps view**

Main View
---------

When you launch the application this is the first view.

.. image:: img/MainView.gif
      :width: 50%
      :align: center

On top you have a blue "stripe" with the name of the application, a "+" and a "gearbox" icons. What are the
functionalities of those icons, we will see in the *Setting a processing scheme* part.

Next is the **Constellation status** header and the information below of it shows you what GNSS constellations and how many satellites are used to compute PVT. In the GIF above we can see that a combination of Galileo+GPS, GPS only and Galileo only are considered in the algorithms. Moreover, you can notice that not all *Visible* satellites are being *Used* in the calculations.
The reason behind this is exaplined in a dedicated chapter of this documentation called *Android GNSS raw measurements*. Shortly,
it is because not all obtained pseudoranges pass a criteria that would allow them to be used in the PVT estimation.

Below the header called **Calculation results** are the results of the PVT estimators (EKF for this particular example) in terms of: latitude ( *Lat* ), longitude ( *Lon* ), altitude ( *Alt* ) and the receiver's clock bias ( *C.bias* ). The UI allows it's user to make some interesting analysis
and to gain some intuitions about the importance of the number of the used satellites in PVT. As example, because there are only 3 Galileo satellites used in the EKF we do expect the estimations of the unknowns to be degraded, which is the case.

And lastly, the *START RAW LOG* allows the logging of the Android GNSS raw measurements in the exact same format as the
Google's Application `GNSS Logger`_. This feature allows you also to do analyze your data in post-processing!

To get to the next view just swipe from right to left.


Satellite signal strength
--------------------------

This view is quite straight forward. Here you can monitor the signal strenght of the satellites that are *Used*
in the calculations.

.. image:: img/SatelliteSignalStrenght.gif
      :width: 50%
      :align: center

To get to the next view just swipe from right to left or to return to the previous one, from left to right.

Positioning error plot
----------------------

To have an idea of how well the position is estimated, we provide this view that contains a plot with the horizontal
position errors using as reference the *Android FINE location* (i.e., the best location output by the phone). The
errors are expressed in meters in the north and east direction (local frame).

.. image:: img/PosErrorPhone.gif
      :width: 50%
      :align: center

Below the plot there is the legend with the specific colors for the chosen processing schemes/configurations.

Google Maps view
----------------

Setting a processing scheme
===========================




.. _`GNSS Logger`: https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger
