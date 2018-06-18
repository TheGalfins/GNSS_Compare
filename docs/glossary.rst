
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


.. _pvt:

Position, Velocity and Time
---------------------------

In GNSS systems we are mostly interested in three parameters of the user. His position, velocity and time. Position is quite obvious - that's the whole point of navigation, to know where the user is located. Velocity can be estimated from consecutive postion measurements, but can also be calculated directly from the satellite signals, due to the Doppler's effect. It can be later used for more precise estimations of the user's position, for highly dynamic systems. Time is also crucial, as the user's receiver is usually not very precise.


Android Glossary
================


Software Engineering Glossary
=============================

.. _polymorphism:

Polymorphism
------------

According to Wikipedia_, *Polymorphism is the provision of a single interface to entities of different types*. In Java this is achieved due to class inheritance.


.. _Wikipedia: https://en.wikipedia.org/wiki/Polymorphism_(computer_science)
