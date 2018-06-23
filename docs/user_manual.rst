
***********
User manual
***********

In order to fully understand what's under the hood for GNSS Compare, we must introduce a few terms we are using through the application. It's going to be a very general explanation -- if something will be not clear, please take a look at our :ref:`Glossary`. We hope you'll find answers to more detailed questions there.

Let's start with the basics. GNSS compare allows the user to calculate the phone's location, based on the phone's GNSS pseudorange measurements. This is normally done by the phone automatically, however with the recent release of Android API 24 and the `GnssMeasurement`_ class, developers gained access to unprocessed pseudorange measurements. This can be used e.g., by GNSS scientists and researchers to come up with new, more precise or less resource intense methods for precise positioning. GNSS Compare is basically a tool for such scientists to compare their algorithms. And if you're not an GNSS expert, it can be a tool for you to learn more on this subject. And believe me -- there's a lot of interesting things to learn.

Constellation
=============

There are a few :ref:`Global Navigation Satellite Systems<gnss>` on the planet. Or actually - around the planet: the `Global Positioning System`_ (GPS), owned by the US Government, `Glonass`_, owned by the Russian Federation, Chineese system `BeiDou`_, and of course `Galileo`_, owned by the European Union, which is set to soon become fully operational.

Each of those constellations consists of a set of 20-30 satellites orbiting above our heads at an altitude of around 20000 km. So many satellites are needed, because the system operator assures that from each spot on Earth, at any time of the day, at least four satellites are visible (and usually a lot more). Those satellites are constantly broadcasting a signal, which among other parameters contains a timestamp. The receiver then receives the signal, compares the timestamp with current time, and thanks to that is able to calculate the distance to the satellite. Knowing the time of transmission and the satellites orbital parameters (retrieved from a special server in the form of :ref:`ephemeris<ephemeris>` data or extracted from the received signal iteself) it's also possible to calcualte the satellite's current position in space. Of course - the details of how those calculations are performed vary slightly from constellation to constellation.

In the context of GNSS Compare, a ``Constellation``, is a class, which defines those two properties:

- is capable of converting raw measurements, extracted from the phone, into pseudoranges
- is able to calculate satellite's current position in the time of transmission.

In this context, the constellations can be treated individually, e.g. we separate GPS from Galileo, and perform position determinating calculations for them separately, or they can be treated together, as in our ``Galileo+GPS`` example. Developers must take care - combining constellations is not always that easy!


Corrections
===========

As the signal travels from the satellite, it's prone to a number of distortions and interferences, which bend it's path and make it appear more distant or closer. For a more accurate positioning, we need to estimate the distance to the satellite with a precision of few meters over thousands of kilometers, so we need to remove from the signal any disturbances we might know of.

Those disturbances are estimated using various, more or less complicated, mathematical models of the natural phenomena. Those models allow us to calculate corrections, which are later applied to the pseudorances. The most commonly used corrections are ionospheric, tropospheric and those including relativistic effects.

In the context of GNSS Compare, a ``Correction`` is a class, which provides a method to calculate the value of the correction, based on few parameters, which include:

- current time,
- receiver's position,
- the satellite's position,
- additional data, stored in the :ref:`ephemeris<ephemeris>` data

The general rule is simple -- the more corrections are applied, the more accurate the final position.


PVT Estimator
=============

The idea is quite simple. PVT estimators are algorithms which take as input satellite positions and pseudoranges to those satellites and aim to estimate the receiver's position, velocity and time. In fact, the simpler ones estimate just the position and time. Let's take a look at the parameters we wish to estimate. Position is quite obvious - that's what we would want to get from this whole process. In some cases, we can use the signal characteristics to improve the estimation of the receiver's velocity (e.g., using doppler measurements), thus increasing the accuracy of the position estimations. And finally, we need to estimate the receiver clock bias with respect to the used satellite navigation system.

.. _`GnssMeasurement`: https://developer.android.com/reference/android/location/GnssMeasurement
.. _`Global Positioning System`: https://www.gps.gov/
.. _`Glonass`: https://www.glonass-iac.ru/en/
.. _`BeiDou`: http://en.chinabeidou.gov.cn/
.. _`Galileo`: https://www.gsa.europa.eu/european-gnss/galileo/galileo-european-global-satellite-based-navigation-system
