
***********
User manual
***********

In order to fully understand what's under the hood for GNSS Compare, we must introduce a few terms we are using through the application. It's going to be a very rough and general explanation - if something will be not clear, please take a look at our :ref:`Glossary`. We hope you'll find answers to more detailed questions there.

Let's start with the basics. GNSS compare allows the user to calculate the phone's location, based on phone's measurements of satellite signals. This is normally done by the phone automatically, but with the recent release of `Android API 24`_, developers gained access to unprocessed measurements. This can be used e.g. by GNSS scientists and researchers to come up with new, more precise or less resource intense methods for precise positioning. GNSS Compare is basically a tool for such scientists to compare their algorithms. And if you're not an GNSS expert, it can be a tool for you to learn more on this subject. And believe me - there's a lot of interesting things to learn.

Constellation
=============

There are a few :ref:`Global Navigation Satellite Systems<gnss>` on the planet. Or actually - around the planet: the `Global Positioning System`_ (GPS), owned by the US Government, `Glonass`_, owned by the Russian Federation, Chineese system `BeiDou`_, and of course `Galileo`_, owned by the European Union, which is set to soon become fully operational.

Each of those constellations consists of a set of 20-30 satellites zooming above our heads at an altitude of around 20,000 km. So many satellites are needed, because the system operator assures that from each spot on Earth, at any time of the day, at least four satellites are visible (and usually a lot more). Those satellites are constantly streaming a signal, which contains a timestamp. The receiver then receives the signal, compares the timestamp with current time, and thanks to that is able to calculate the distance to the satellite. This distance is called a :ref:`pseudorange<pseudorange>`. Knowing the time of transmission and satellites orbital parameters (retrieved from a special server) it's also possible to calcualte the satellite's current position in space. Of course - the details of how those calculations are performed vary slightly from constellation to constellation.

In the context of GNSS Compare, a *Constellation*, is an object, which defines those two properties:

- is capable of converting raw measurements, extracted from the phone, into pseudoranges
- is able to calculate satellite's current position in the time of transmission.

In this context, the constellations can be treated individually, e.g. we separate GPS from Galileo, and perform position determinating calculations for them separately, or they can be treated together, as in our ``Galileo+GPS`` example. Developers must take care - combining constellations is not always that easy!



.. _`Android API 24`: https://developer.android.com/reference/android/location/GnssMeasurement
.. _`Global Positioning System`: https://www.gps.gov/
.. _`Glonass`: https://www.glonass-iac.ru/en/
.. _`BeiDou`: http://en.chinabeidou.gov.cn/
.. _`Galileo`: https://www.gsa.europa.eu/european-gnss/galileo/galileo-european-global-satellite-based-navigation-system
