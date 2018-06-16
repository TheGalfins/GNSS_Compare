
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
a *static user* and a *dynamic user*.

Therefore we will describe how the state vector is defined, or in other words, the vector containing the parameters that we wish to estimate
(hint: the parameters are related to the *GNSS Compare*'s user position!), and also what dynamic and measurements models we have considered. And as *bonus* we
will also write about the tunning of the EKFs.

First things first! Let's remember the Kalman Filter equations, *the implemented ones*, in order to make the rest of this section more enjoyable.

We have the time prediction of the state vector (x) and it's variance-covariance matrix (P):

.. math::
  \hat{\mathbf{x}}^-_k = \mathbf{F}_k \hat{\mathbf{x}}^+_{k-1}
.. math::
  \mathbf{P}^-_k = \mathbf{F}_k \mathbf{P}^+_{k-1} \mathbf{F}^{\text{T}}_k + \mathbf{Q}_k .

In the next step we can compute the innovation vector (gamma) and it's variance-covariance matrix (S) with the help of
the obsevation vector (z), the observation matrix (H) and the measurement noise matrix (R):

.. math::
  \boldsymbol{\gamma}_k = \mathbf{z}_k - \mathbf{H}_k\hat{\mathbf{x}}^-_k
.. math::
  \mathbf{S}_k = \mathbf{H}_k \mathbf{P}^-_k \mathbf{H}_k^{\text{T}} + \mathbf{R}_k.

We are almost there, we just need to compute the famous Kalman gain (K)!

.. math::
  \mathbf{K}_k = \mathbf{P}^-_k \mathbf{H}_k^{\text{T}} \mathbf{S}^{-1}_k.

Finally the measurement update step is:

.. math::
  \hat{\mathbf{x}}^+_k = \hat{\mathbf{x}}^-_k + \mathbf{K}_k \boldsymbol{\gamma}_k
.. math::
  \mathbf{P}^+_k = \left(\mathbf{I}_k - \mathbf{K}_k \mathbf{H}_k \right) \mathbf{P}^-_k.

However, before explaining how the EKF for the *static user* and the *dynamic user* was implemented, we still
need to talk about the measurement model based on the GNSS pseudoranges retrieved from the smartphone's GNSS
receiver. If you are familiar with this concepts, you can skip the following section.

Pseudorange measurement model
-----------------------------

For a code-based pseudorange (PRc) we have the following (non-linear) equation taking into account the
satellite clock bias (dtS), the delay caused by the ionosphere (dion), the delay caused by the troposphere (dtrop)
and the receiver noise (epsilon).

.. math::
   PR_c = \rho + \delta t_R - \delta t^S + d_{\text{ion}} + d_{\text{trop}} + \mathbf{\epsilon}

We know, there are more effects that are perturbing the GNSS measurements, however
we wish to keep things as simple as possible and the interested persons can always access some good books on this topic!

The above equation is non-linear because of the geometric distance (rho) between the receiver and the GNSS satellite. Luckly we can
linearize it if we have knowledge about an approximated position of the receiver (X0, Y0, Z0), which we do! We do have from the time prediction
step of the EKF. Taking this into account and applying a first order Taylor series expansion we obtain:

.. math::
  PR_c - \rho_0 + \delta t^S - d_{0,\text{ion}} - d_{0,\text{trop}} = -\frac{X^S-X_0}{\rho_0}\Delta X-\frac{Y^S-Y_0}{\rho_0}\Delta Y-\frac{Z^S-Z_0}{\rho_0}\Delta Z+\delta t_R

On the left side of the equation we have moved every term that can be computed. The subscript 0 means that those parameters are estimated
by using the approximate receiver position information. On the right hand side we have the unknowns (dX, dY, dZ, dtR) and their coefficients. Based on the linearized
pseudorange equation one can form the observation matrix (H).

*Practical advise: Take care that the unknowns from the linearized pseudorange equations are not the same as the position related unknowns
that we are estimating directly in the EKF state vector. Check the GNSS Compare code (e.g., StaticExtendedKalmanFilter class) to understand how this is handled*.

Good, now we can see how the EKF was implemented for the *static user* and the *dynamic user*!

Static user
-----------
In the case of a static user we have the following state vector at the epoch *k*:

.. math::

    \mathbf{x}_k = \left(X~~Y~~Z~~\delta t_R~~\dot{\delta t}_R \right)^{\text{T}}

In the above expression X, Y and Z are the coordinates in Earth Centered Earth Fixed (ECEF) frame and the last two parameters
are the receiver clock bias and the receiver clock drift. All the parameters are expressed in units of meters.

Now that the state vector is defined, we can move on by choosing the dynamic model. However first, let's think a bit about this
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
In the static case we can assume that the user is not moving and that the receiver clock has some frequency and phase errors. In order to
fully understand this reasoning, the interested reader is advised to check the following book: *Introduction to Random Signals and Applied Kalman Filtering*
by Rober Grover Brown and Patrick Y. C. Hwang. Therefore, the process noise matrix is approximated to be:

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

The parameter h-2 and h0 are the Power Spectral Densities (PSD) of the random walk frequency noise and of the white noise, as defined in the suggested book above. Some typical values for a low quality Temperature
Compensated Crystal Oscillator (TCXO) are 2e-20 and 2e-19 (in seconds). A practical advise before using this values
is to take care that we are dealing with the parameters of a variance-covariance matrix and also that they have
to be converted in units of meters (remember that we have expressed the receiver clock states in units of meters).

So basically we are done with the *static user* case! That's great as we can move to the dynamic one!

Dynamic user
-----------------------------

In the case of a dynamic user there are few aspects that one has to consider. Let's start again by defining the new
state vector:

.. math::
    \mathbf{x}_k = \left(X~~U~~Y~~V~~Z~~W~~\delta t_R~~\dot{\delta t}_R \right)^{\text{T}}.

We can already observe that we have three more parameters to estimate (U, V, W) which are the velocities on the X, Y and Z directions.
If our state vector is modified (with respect to the static case) then our intuition will tell us that we need to define a new transition matrix and a
a new process noise matrix. Which is exactly what we are going to do next, therfore:

.. math::
  \mathbf{F}_k =
  \begin{pmatrix}
           1 & \Delta T & 0 & 0 & 0 & 0 & 0 & 0 \\
           0 & 1 & 0 & 0 & 0 & 0 & 0 & 0\\
           0 & 0 & 1 & \Delta T & 0 & 0 & 0 & 0 \\
           0 & 0 & 0 & 1 & 0 & 0 & 0 & 0 \\
           0 & 0 & 0 & 0 & 1 & \Delta T & 0 & 0 \\
           0 & 0 & 0 & 0 & 0 & 1 & 0 & 0 \\
           0 & 0 & 0 & 0 & 0 & 0 & 1 & \Delta T \\
           0 & 0 & 0 & 0 & 0 & 0 & 0 & 1 \\
   \end{pmatrix}


For the process noise matrix we use the approach presented in the book of Rober Grover Brown and Patrick Y. C. Hwang
( *Introduction to Random Signals and Applied Kalman Filtering* ). Indeed, is the second we refer to this book in the implemented PVT algorithms section, however you can trust us that is a very good one!

.. math::
  \mathbf{Q}_k =
  \begin{pmatrix}
           \frac{S_X~\Delta T^3}{3}& \frac{S_X~\Delta T^2}{2}& 0 & 0 & 0 & 0 & 0 & 0 \\
           \frac{S_X~\Delta T^2}{2}& S_X~\Delta T & 0 & 0 & 0 & 0 & 0 & 0 \\
           0~~~~& 0~~~~& \frac{S_Y~\Delta T^3}{3} & \frac{S_Y~\Delta T^2}{2} & 0 & 0 & 0 & 0\\
           0~~~~& 0~~~~& \frac{S_Y~\Delta T^2}{2} & S_Y~\Delta T & 0 & 0 & 0 & 0\\
           0~~~~& 0~~~~& 0 & 0 & \frac{S_Z~\Delta T^3}{3} & \frac{S_Z~\Delta T^2}{2} & 0 & 0\\
           0~~~~& 0~~~~& 0 & 0 & \frac{S_Z~\Delta T^2}{2} & S_Z~\Delta T & 0 & 0\\
           0~~~~& 0~~~~& 0 & 0 & 0 & 0 & S_f+\frac{S_g~\Delta T^3}{3} & \frac{S_g~\Delta T^2}{2} \\
           0~~~~& 0~~~~& 0 & 0 & 0 & 0 & \frac{S_g~\Delta T^2}{2} & S_g~\Delta T \\
   \end{pmatrix}


Filter tunning
-----------------------------

Weighted Least Squares
======================
