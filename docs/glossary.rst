
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
  R = c \cdot [t_{rx}+\delta t_{rx} - (t^{tx} + \delta t^{tx}].





.. _ephemeris:

Ephemeris
---------

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
