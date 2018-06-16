
**********************
Implemented Algorithms
**********************


Kalman Filter
=============

One of the estimation techniques implemented in the *GNSS Compare* framework is the Kalman Filter.
Taking into account that the measurement model is linearized about the time predicted position, in fact the implementation
is an Extended Kalman Filter (EKF).

In this section we describe the theoretical aspects of the EKF implementation such that the curious minds can understand easily
what is behind GNSS Compare's awesome algorithms.

Therefore we begin with the definition of the state vector, or in other words, the vector containing the parameters that we wish to estimate
(hint: the parameters are related to the *GNSS Compare*'s user position!). We are interested in to implement the EKF for two types of users:
a *static user* and *dynamic user*.


Static user
-----------

.. math::

    \mathbf{x} = \left( \right)


This is further text
