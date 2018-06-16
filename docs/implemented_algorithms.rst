
***************************
Implemented PVT Algorithms
***************************


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

First things first! Let's remember the Kalman Filter equations, *the implemented ones*, in order to make the rest of this section more enjoyable.

We have the time prediction of the state vector (P) and it's variance-covariance matrix (P)

.. math::
  \hat{\mathbf{x}}_k = \mathbf{F}_k \hat{\mathbf{x}}_{k-1}
.. math::
  \mathbf{P}^-_k = \mathbf{F}_k \mathbf{P}^+_{k-1} \mathbf{F}^{\text{T}}_k + \mathbf{Q}_k  


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
    \delta t_{R,k} = \delta t_{R,k-1} + \Delta T~\dot{\delta t}_{R,k-1}
.. math::
      \dot{\delta t}_{R,k} = \dot{\delta t}_{R,k-1}

Having in view all of this information we can define the transition matrix (F) of the filter as:

.. math::
  \mathbf{F}_k =
  \begin{pmatrix}
           1 & 0 & 0 & 0 & 0 \\
           0 & 1 & 0 & 0 & 0 \\
           0 & 0 & 1 & 0 & 0 \\
           0 & 0 & 0 & 1 & \Delta T \\
           0 & 0 & 0 & 0 & 1 \\
   \end{pmatrix}

We are almost done with the dynamic model elements. The only thing that we need now is the process noise matrix (Q). Because
the process noise matrix contains the uncertainty we have in the dynamic model that we consider, we have to define it accordingly.
In the static case we are sure that the user is not moving and that the receiver clock has frequency and phase errors. In order to
fully understand this reasoning, the interested reader is advised to check the following book: *Introduction to Random Signals and Applied Kalman Filtering*
by Rober Grover Brown and Patrick Y. C. Hwang. Therefore, the process noise matrix is:

.. math::
  \mathbf{Q}_k =
  \begin{pmatrix}
           0~~~~& 0~~~~&0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & S_f+\frac{S_g~\Delta T^3}{3} & \frac{S_g~\Delta T^2}{2} \\
           0~~~~& 0~~~~& 0 & \frac{S_g~\Delta T^2}{2} & S_g~\Delta T \\
   \end{pmatrix}

In the above expression the receiver clock related parameters are expressed as:

.. math::
  S_g \approx 2 \pi^2 h_{-2}
.. math::
  S_f \approx \frac{h_0}{2}

The parameter h-2 and h0 are the Power Spectral Density (PSD) of the random walk frequency noise and the PSD
of the white noise, as defined in the suggested book above. Some typical values for a low quality Temperature
Compensated Crystal Oscillator (TCXO) are 2e-20 and 2e-19 (in seconds). A practical advise before using this values
is to take care that we are dealing with the parameters of a variance-covariance matrix and also that they have
to be converted in units of meters (remember that we have expressed the receiver clock states in units of meters).

So basically we are done with the *static user* case! That's great as we can move to the dynamic one!
