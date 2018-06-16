
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
(hint: the parameters are related to the *GNSS Compare*'s user position!), and also what dynamic and measurements models we have considered. And as *bonus* we
will also write about the tunning of the EKFs.


Static user
-----------
In the case of a static user we have the following state vector at the epoch *k*:

.. math::

    \mathbf{x}_k = \left(X~~Y~~Z~~\delta t_R~~\dot{\delta t}_R \right)^{\text{T}}

In the above expression X, Y and Z are the coordinates in Earth Centered Earth Fixed (ECEF) frame and the last two parameters
are the receiver clock bias and the receiver clock drift. All the parameters are expressed in units of meters.

Now that the state vector is defined, we can move on choosing the dynamic model. However first, let's think a bit about this
aspect. A static user doesn't *change* his/hers position, therefore this means that over time the X, Y, Z coordinates remain
the same! We only have to take care of how we model the dynamic behavior of the receiver's clock, which is approximated to be:

.. math::
    \delta t_{R,k} = \delta t_{R,k-1} + \Delta T~\dot{\delta t}_{R,k-1} \text{with} \dot{\delta t}_{R,k} = \dot{\delta t}_{R,k-1}

Having in view all of this information we can define the transition matrix of the filter as:

.. math::
  \mathbf{F}_k =
  \begin{pmatrix}
           1 & 0 & 0 & 0 & 0 \\
           0 & 1 & 0 & 0 & 0\\
           0 & 0 & 1 & 0 & 0\\
           0 & 0 & 0 & 1 & \Delta T\\
           1 & 0 & 0 & 0 & 1\\
   \end{pmatrix}
