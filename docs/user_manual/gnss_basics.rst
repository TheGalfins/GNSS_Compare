
GNSS basics
===========


Constellation
-------------

There are a few :ref:`Global Navigation Satellite Systems<gnss>` on the planet. Or actually - around the planet: the `Global Positioning System`_ (GPS), owned by the US Government, `GLONASS`_, owned by the Russian Federation, Chineese system `BeiDou`_, and of course `Galileo`_, owned by the European Union, which is set to soon become fully operational.

Each of those constellations consists of a set of 20-30 satellites orbiting above our heads at an altitude of around 20000 km. So many satellites are needed, because the system operator assures that from each spot on Earth, at any time of the day, at least four satellites are visible (and usually a lot more). Those satellites are constantly broadcasting a signal, which among other parameters contains a timestamp. The receiver then receives the signal, compares the timestamp with the current time, and thanks to that is able to calculate the distance to the satellite. Knowing the time of transmission and the satellites orbital parameters (retrieved from a special server in the form of :ref:`ephemeris<ephemeris>` data or extracted from the received signal iteself) it's also possible to calcualte the satellite's position in space. Of course, the details of how those calculations are performed vary slightly from constellation to constellation.

In the context of GNSS Compare, a ``Constellation``, is a class, which defines those two properties:

- is capable of converting the raw measurements extracted from the phone into pseudoranges,
- is able to calculate satellite's position in the time of transmission.

In this context, the constellations can be treated individually, e.g. we separate GPS from Galileo, and perform position determinating calculations for them separately, or they can be treated together, as in our ``Galileo+GPS`` example. Developers must take care as combining constellations is not always that easy!


Corrections
-----------

As the signal travels from the satellite, it's prone to a number of sources of error (e.g., ionosphere, troposphere), which the user will have to take into account. For a more accurate positioning, we need to estimate the distance to the satellite with as good as possible, so we need to remove from the signal any disturbances we might know of.

Those disturbances are estimated using various, more or less complicated, mathematical models of the natural phenomena. Those models allow us to calculate corrections, which are later applied to the pseudoranges. The most commonly used corrections are for the ionosphere, troposphere and for those including the relativistic effects.

In the context of GNSS Compare, a ``Correction`` is a class, which provides a method to calculate the value of the correction, based on few parameters, which include:

- time of signal reception,
- receiver's approximate position,
- the satellite's position,
- additional data, stored in the :ref:`ephemeris<ephemeris>` data

The general rule is simple -- the more corrections are applied, the more accurate the final position.


PVT Estimator
-------------

The PVT estimators are algorithms which take as input satellite positions and pseudoranges to those satellites and aim to estimate the receiver's position, velocity and time. In some applications, it's sufficient to estimate just the position and time. Let's take a look at the parameters we wish to estimate. Position is quite obvious - that's what we would want to get from this whole process. In some cases, we can use the signal characteristics to improve the estimation of the receiver's velocity (e.g., using doppler measurements), thus increasing the accuracy of the position estimations. Additionally to the position related parameters we also need to estimate the receiver :ref:`clock bias<clockBias>` with respect to a certain GNSS time frame (e.g., Galileo System Time). This is handled by having the clock bias as one of the paramters to be estimated alongside with the position and velocity.

In the context of GNSS Compare, the ``PvtMethod`` class does exactly that. It's supposed to calculate the receiver's position, based on observed satellite parameters. Internally, it should be storing the calculated velocity and clock bias for enhanced processing, but from the point of view of GNSS Compare's framework, at the moment, the only value used outside of the ``PvtMethod`` class is the calculated position. But hey -- there's of course room to improve.



.. _`Global Positioning System`: https://www.gps.gov/
.. _`GLONASS`: https://www.glonass-iac.ru/en/
.. _`BeiDou`: http://en.chinabeidou.gov.cn/
.. _`Galileo`: https://www.gsa.europa.eu/european-gnss/galileo/galileo-european-global-satellite-based-navigation-system
