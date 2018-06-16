
**********************
Implemented Algorithms
**********************


Kalman Filter
=============

One of the estimation techniques implemented in the *GNSS Compare* framework is the Kalman Filter.
Taking into account that the measurement model is linearized about the time predicted position, in fact the implementation
is an Extended Kalman Filter (EKF).

In this section we describe the theoretical aspects of the EKF implementation such that the curious minds can understand easily
what is behind GNSS Compare's awesome algorithms. We are interested to implement the EKF for two types of users:
a *static user* and *dynamic user*.

Therefore we will describe how the state vector is defined, or in other words, the vector containing the parameters that we wish to estimate
(hint: the parameters are related to the *GNSS Compare*'s user position!), the considered dynamic and measurements models. And as *bonus* we
will also write about the (dreadful) tunning of the EKFs.


Static user
-----------

.. math::

    \mathbf{x} = \left( \right)


This is further text
