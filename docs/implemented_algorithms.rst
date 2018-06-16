
**********************
Implemented Algorithms
**********************


Kalman Filter
=============

One of the estimation techniques implemented in the GNSS Compare framework is the Kalman Filter.
Taking into account that the measurement model is linearized about the time predicted position, the implementation
is an Extended Kalman Filter (EKF).

.. math::

    (a + b)^2 = a^2 + 2ab + b^2


This is further text
