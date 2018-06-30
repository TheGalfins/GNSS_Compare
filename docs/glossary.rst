
********
Glossary
********


This section is to provide a small glossay on the subject of satellite navigation, Android and software engineering in general. You can find below short descriptions of all used technical terms in this documentation.


Global Navigation Satellite Systems Glossary
============================================


.. _gnss:

Global Navigation Satellite Systems
-----------------------------------

.. _pseudorange:

Pseudorange
-----------

For someone making his or hers first steps in the GNSS field, the term *pseduorange* might sound a little bit confusing. Afterall, the word *pseudo* is synonym with *false* and considering this, one might ask: why use this type of information? At the end of this section we hope to answer this question and also to make things more clear regarding this subject.

First let's start thinking (in general terms) how the receiver determines the distances towards the observed satellites. The range (R) is the difference between the time of signal reception and the time of signal transmission multiplied by the speed of light (c):

.. math::
  R = c \cdot (t_{rx} - t^{tx}).

Although the clocks (atomic clocks) of the satellites are highly accurate, they are still not perfect which lead them to be biased with respect to a certain GNSS System Time. Furthermore, considering that the quality of the clocks used in the typical GNSS receiver is inferior to the ones of the satellites, there is also a (significantly larger) bias in its time measurements. Therefore, let's take this into account in our equation expressed above:

.. math::
  R = c \cdot [t_{rx}+\delta t_{rx} - (t^{tx} + \delta t^{tx})].

If we arrange a bit the newly obtained expression, we get:

.. math::
  R = c \cdot (t_{rx}-t^{tx})+ c \cdot (\delta t_{rx} - \delta t^{tx}).

Assuming that the time of signal reception and the time of signal transmission are free of their biases and other error sources, then their difference multiplied by the speed of light can be viewed as the equivalent of the geometric distance (rho) in 3D between the receiver and the observed satellite!

.. math::
  R = \rho + c \cdot (\delta t_{rx} - \delta t^{tx}).

Now that we got this settled, we also need to account for the effects that disturb the signal's travel from the satellite to the receiver such as the ionosphere (I), troposphere (T) and for the local effects like the receiver's noise, multipath which for the sake of simplicity we gather these terms in a single one (epsilon). The number of effects that introduce errors in the range measurements is larger and we don't cover them here.

.. math::
  R = \rho + c \cdot (\delta t_{rx} - \delta t^{tx}) + I + T + \epsilon.

In the equation of the range above we correct for the effect of the satellite clock bias, ionosphere, troposphere mainly by mathematical models. However, what we can't remove directly is the receiver clock bias which is required to be estimated. And that term will always be present in our measurements! Therefore, our *range* equation becomes the *pseudorange* (PR) equation because of that.

.. math::
  PR = \rho + c \cdot (\delta t_{rx} - \delta t^{tx}) + I + T + \epsilon.

We do hope that the aspects related to this subject are more clear now.




.. _ephemeris:

Ephemeris
---------

The process of obtaining the position in a certain coordinate system using GNSS technologies is based on a rather simple principle, which is *trilateration* (not triangulation, please be aware of that). Given an unknown point in a coordinate sytem from which we know the distances towards some known points in the same coordinate system, we can work out the coordinates of our unknown point. One can try this concept by defining a 2D coordinate system in which a triangle can be drew with two of its verticies having known coordinates. And the problem relies on finding the coordinates of the third vertex.

We have already seen in the *Pseudorange* section that we can obtain the range information towards the observed satellites. And what is missing is how to determine the coordinates of those satellites. To compute the coordinates of the satellite we need some parameters that describe their orbits. For this we have to be grateful to the work of Johannes Kepler on his law of planetary motion as he discovered the six parameters also known as the *Keplerian elements* that define an orbit:

- Eccentricity

- Semimajor axis

- Inclination

- Longitude of the ascending node

- Argument of periapsis

- True anomaly

The definition of...


.. _pvt:

Position, Velocity and Time
---------------------------

In GNSS we are mostly interested in the parameters of the user that describe the position, velocity and also time. Position is quite obvious - that's the whole point of navigation, to know where the user is located. Velocity can be estimated from consecutive postion measurements, but can also be calculated directly from the satellite signals, due to the Doppler's effect. It can be later used for more precise estimations of the user's position, for highly dynamic systems. Time is also crucial, as the user's receiver contains a bias that has to be estimated.





.. _clockBias:

Clock bias
----------


Android Glossary
================


Software Engineering Glossary
=============================

.. _polymorphism:

Polymorphism
------------

According to Wikipedia_, *Polymorphism is the provision of a single interface to entities of different types*. In Java this is achieved due to class inheritance.


.. _Wikipedia: https://en.wikipedia.org/wiki/Polymorphism_(computer_science)
