
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

Next is the **Constellation status** header that tells you what GNSS constellations and how many satellites are used to compute
PVT. In the example above we can see that a combination of Galileo+GPS, GPS only and Galileo only are considered
in the algorithms. Moreover, you can see that there that not all *Visible* satellites are being *Used* in the calculations.
The reason behind this is exaplined in a dedicated chapter of this documentation called *Android GNSS raw measurements*. Shortly,
it is because not obtained pseudoranges pass a criteria that would allow them to be used in the PVT estimation.

Satellite signal strength
--------------------------

Positioning error plot
----------------------

Google Maps view
----------------

Setting a processing scheme
===========================
