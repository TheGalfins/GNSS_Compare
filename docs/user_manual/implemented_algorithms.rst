
***************************
Implemented PVT Algorithms
***************************

In this section we provide the theoretical aspects behind the *GNSS Compare's* PVT algorithms.
The information here can be associated with the following Java classes:

- ``StaticExtendedKalmanFilter``

- ``DynamicExtendedKalmanFilter``

- ``PedestrianStaticExtendedKalmanFilter`` - this one sounds a bit strange, however bear with us as explanations will be given when the filter tunning is explained

- ``WeightedLeastSquares``

Extended Kalman Filter
======================

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
the observation vector (z), the observation matrix (H) and the measurement noise matrix (R):

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
receiver. If you are familiar with these concepts, you can skip the following section.

Pseudorange measurement model
-----------------------------

For a code-based pseudorange (PRc) we have the following (non-linear) equation taking into account the
satellite clock bias (dtS), the delay caused by the ionosphere (dion), the delay caused by the troposphere (dtrop)
and the receiver noise (epsilon).

.. math::
   PR_c = \rho + \delta t_R - \delta t^S + d_{\text{ion}} + d_{\text{trop}} + \mathbf{\epsilon}

We know, there are more effects that are perturbing the GNSS measurements, however
we wish to keep things as simple as possible and the interested persons can always consider more error sources in the *GNSS Compare's* code.

The above equation is non-linear because of the geometric distance (rho) between the receiver and the GNSS satellite. Luckily we can
linearize it if we have knowledge about an approximated position of the receiver (X0, Y0, Z0), which we do! We do have from the time prediction
step of the EKF. Taking this into account and applying a first order Taylor series expansion we obtain:

.. math::
  PR_c - \rho_0 + \delta t^S - d_{0,\text{ion}} - d_{0,\text{trop}} = -\frac{X^S-X_0}{\rho_0}\Delta X-\frac{Y^S-Y_0}{\rho_0}\Delta Y-\frac{Z^S-Z_0}{\rho_0}\Delta Z+\delta t_R.

On the left side of the equation we have moved every term that can be computed. The subscript 0 means that those parameters are estimated
by using the approximate receiver position information. On the right hand side we have the unknowns (dX, dY, dZ, dtR) and their coefficients. Based on the linearized
pseudorange equation one can form the observation matrix (H).

*Practical advice: Take care that the unknowns from the linearized pseudorange equations are not the same as the position related unknowns that we are estimating directly in the EKF state vector. Check the GNSS Compare code ( e.g.,* ``StaticExtendedKalmanFilter`` *class ) to understand how this is handled*.

Good, now we can see how the EKF was implemented for the *static user* and the *dynamic user*!

Static user
-----------

In the case of a static user we have the following state vector at the epoch *k*:

.. math::

    \mathbf{x}_k = \left(X~~Y~~Z~~\delta t_R~~\dot{\delta t}_R \right)^{\text{T}}.

In the above expression X, Y and Z are the coordinates in Earth Centered Earth Fixed (ECEF) frame and the last two parameters
are the receiver clock bias and the receiver clock drift. All the parameters are expressed in units of meters.

Now that the state vector is defined, we can move on by choosing the dynamic model. Before moving further let's think a bit about this aspect. A static user doesn't *change* his/hers position, therefore this means that over time the X, Y, Z coordinates remain
the same! We only have to take care of how we model the dynamic behavior of the receiver's clock, which is approximated to be:

.. math::
    \delta t_{R,k} = \delta t_{R,k-1} + \Delta T~\dot{\delta t}_{R,k-1},
.. math::
      \dot{\delta t}_{R,k} = \dot{\delta t}_{R,k-1}.

Having in view all of this information we can define the transition matrix (F) of the filter as:

.. math::
  \mathbf{F}_k =
  \begin{pmatrix}
           1 & 0 & 0 & 0 & 0 \\
           0 & 1 & 0 & 0 & 0 \\
           0 & 0 & 1 & 0 & 0 \\
           0 & 0 & 0 & 1 & \Delta T \\
           0 & 0 & 0 & 0 & 1 \\
   \end{pmatrix}.

We are almost done with the dynamic model elements. The only thing that we need now is the process noise matrix (Q). Because
the process noise matrix contains the uncertainty we have in the dynamic model that we consider, we have to define it accordingly.
In the static case we can assume that the user is not moving and that the receiver clock has some frequency and phase errors. In order to
fully understand this reasoning, the interested reader is advised to check the following book: *Introduction to Random Signals and Applied Kalman Filtering by Robert Grover Brown and Patrick Y. C. Hwang*.Therefore, the process noise matrix is approximated to be:

.. math::
  \mathbf{Q}_k =
  \begin{pmatrix}
           0~~~~& 0~~~~&0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & S_f+\frac{S_g~\Delta T^3}{3} & \frac{S_g~\Delta T^2}{2} \\
           0~~~~& 0~~~~& 0 & \frac{S_g~\Delta T^2}{2} & S_g~\Delta T \\
   \end{pmatrix}.

In the above expression the receiver clock related parameters are expressed as:

.. math::
  S_g \approx 2 \pi^2 h_{-2},
.. math::
  S_f \approx \frac{h_0}{2}.

The parameter h-2 and h0 are the Power Spectral Densities (PSD) of the random walk frequency noise and of the white noise, as defined in the suggested book above. Some typical values for a low quality Temperature
Compensated Crystal Oscillator (TCXO) are 2e-20 and 2e-19 (in seconds). A practical advice before using this values
is to take care that we are dealing with the parameters of a variance-covariance matrix and also that they have
to be converted in units of meters (remember that we have expressed the receiver clock states in units of meters).

So basically we are done with the *static user* case. That's great as we can move to the dynamic one!

Dynamic user
-----------------------------

In the case of a dynamic user there are few aspects that one has to consider. Let's start again by defining the new
state vector:

.. math::
    \mathbf{x}_k = \left(X~~U~~Y~~V~~Z~~W~~\delta t_R~~\dot{\delta t}_R \right)^{\text{T}}.

We can already observe that we have three more parameters to estimate (U, V, W) which are the velocities on the X, Y and Z directions.
If our state vector is modified (with respect to the static case) then our intuition will tell us that we need to define a new transition matrix and a
new process noise matrix. Which is exactly what we are going to do next, therefore:

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
   \end{pmatrix}.


For the process noise matrix we use the approach presented in the book of Robert Grover Brown and Patrick Y. C. Hwang
( *Introduction to Random Signals and Applied Kalman Filtering* ). Indeed, it is the third time we refer to this book in the implemented PVT algorithms section, however you can trust us that it is a very good one!

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

The parameters Sx, Sy and Sz are the spectral amplitudes that reflect the position random process. Unfortunately, setting their values
is not as straightforward as for the receiver clock states. We have to rely on what we call the *tunning* process which is
modifying the values in Q and R experimentally (i.e., trial and error). Just as a note, this can be avoided by designing and
implementing *adaptive* estimators. Who knows, maybe you (the reader) will decide to implement some nice ideas now that
this possibility is enabled with *GNSS Compare's* flexible framework.

*Practical advice: When the observation matrix (H) is being built do consider that it's size is defined in the following way:
the number of rows is the number of measurements and the number of columns is the number of unknowns. Therefore when
switching from the static case to the dynamic case, H changes also. We mention this just to be sure that a possible conceptual hiccup
is avoided.*

Filter tunning
-----------------------------

Because at the moment we are dealing with a standard EKF and not an adaptive one this means that we have to
assign values in the process noise matrix (Q) and in the measurement noise matrix (R) such that the filter
is tunned to our situation.

Let's start with the R matrix. We set R to be a diagonal matrix containing the variances of each pseudorange measurement.
The measurement noise matrix being diagonal relies on the assumption that there is no cross-correlation between the measurements
coming from different satellites ( *an assumption that is not entirely representing the reality, however it fits most of the
applications* ). Therefore, the diagonal elements of the R matrix are:

.. math::
  \mathbf{R}_{ii,k} = \sigma^2_{ii}.

To keep things relatively simple, we can assign the value for the sigma 10 meters ( *don't forget to square it before putting it in R* ).
Another assumption that is made is that the measurements received at the k-th epoch have equal variances ( *ok, this assumption is not
true at all* ). However here is an idea for you, maybe you can try investigating some interesting measurement weighing methods and then
*compare* (the main keyword of the whole project) the results you get with our not so realistic assumption. Let the researcher within you thrive!

Let's move to the Q matrix now. For this we present three tunning examples: static, pedestrian and dynamic.

**Static tunning**

For the static case we have already seen that we only have to take care about the process noise of the receiver clock states. So the values
that we are assigning to the PSD of the random walk of the frequency noise and of the white noise are:

.. math::
  h_{-2} = 2e-20~c^2,
.. math::
  h_0 = 2e-19~c^2.

In the above we use the *c* notation for the speed of light.

**Pedestrian tunning**

Intuitively we should have used the EKF designed for a dynamic user in this situation. It would only make sense as a pedestrian *changes* his/hers
position over time. However, one must take into account that the raw measurements delivered by the smartphone's GNSS receiver are quite noisy and if
there are no other means to detect the motion of the user (e.g., using an Inertial Measurement Unit) then estimating the velocities can make our results not too accurate.
Having this situation in view we have found a workaround: we use the EKF designed for a static user and we let some process noise for the X and Y coordinates ( *unless
one of our users is not Superman we are not that interested in the Z direction* ). This means that we have the following Q matrix:

.. math::
  \mathbf{Q}_k =
  \begin{pmatrix}
           0.2~~~~& 0~~~~&0 & 0 & 0 \\
           0~~~~& 0.2~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & 0 & 0 \\
           0~~~~& 0~~~~& 0 & S_f+\frac{S_g~\Delta T^3}{3} & \frac{S_g~\Delta T^2}{2} \\
           0~~~~& 0~~~~& 0 & \frac{S_g~\Delta T^2}{2} & S_g~\Delta T \\
   \end{pmatrix}.

The value 0.2 was chosen by trial and error and it fits a *slow* walking pedestrian. We hope that the name of the Java class ``PedestrianStaticExtendedKalmanFilter`` makes a little bit more sense now.

**Dynamic tunning**

Finally we have arrived at the final case regarding the tunning of the dynamic EKF. Again the following values were determined empirically:

.. math::
  S_X = S_Y = 0.8,
.. math::
  S_Z = 0.08.


Weighted Least Squares
======================

*GNSS Compare* offers also the possibility to change the PVT estimator if the user wishes to. By not requiring knowledge about
the dynamics, Weighted Least Squares (WLS) can be used to estimate the position using only the pseudorange measurements. However there
are some drawbacks like: the quality of the estimations fully depends on the quality of the measurements and also the WLS
requires a minimum number of measurements (typically 4 if we want to estimate the 3D position and the receiver clock bias).

Nevertheless it is useful to have such an estimator as its behavior can be studied in real-time/post-processing in comparison with an EKF.
And all this thanks to *GNSS Compare*!

Although the pseudorange measurement model was presented in the EKF description we will do it one more time just for the
sake of completion.

Pseudorange measurement model
-----------------------------

The linearized code-based pseudorange measurement is:

.. math::
  PR_c - \rho_0 + \underbrace{(\delta t^S - d_{0,\text{ion}} - d_{0,\text{trop}})}_{Corr}
  = -\frac{X^S-X_0}{\rho_0}\Delta X-\frac{Y^S-Y_0}{\rho_0}\Delta Y-\frac{Z^S-Z_0}{\rho_0}\Delta Z+\delta t_R.

Let's also express the unit line of sight vector and the position related unknowns as:

.. math::
    \mathbf{u} = \left[-\frac{X^S-X_0}{\rho_0},~~-\frac{Y^S-Y_0}{\rho_0},~~-\frac{Z^S-Z_0}{\rho_0} \right],
.. math::
    \delta\mathbf{r} = \left[\Delta X,~~\Delta Y,~~\Delta Z \right].

For *n* observed satellites we have the following measurement model:

.. math::
  \underbrace{\begin{pmatrix}
           PR^1_c - \rho^1_0 + Corr^1 \\
           PR^2_c - \rho^2_0 + Corr^2\\
           \vdots \\
           PR^n_c - \rho^n_0 + Corr^n\\
   \end{pmatrix}}_{\mathbf{z}}
   =
   \underbrace{\begin{pmatrix}
            \mathbf{u}^1 & 1\\
            \mathbf{u}^2 & 1\\
            \vdots & \vdots \\
            \mathbf{u}^n & 1\\
    \end{pmatrix}}_{\mathbf{H}}
    \underbrace{\begin{pmatrix}
             \delta \mathbf{r}^{\text{T}} \\
             \delta t_R\\
     \end{pmatrix}}_{\mathbf{x}}.

To estimate the vector of unknowns (x) in the WLS sense, we proceed in the following way:

.. math::
  \hat{\mathbf{x}}_{\text{WLS}} = \left(\mathbf{H}^{\text{T}} \mathbf{W} \mathbf{H} \right)^{-1}\mathbf{H}^{\text{T}} \mathbf{W} \mathbf{z}.

*Practical advice: In the WLS case, as the position is concerned, we are estimating the difference between the approximated position and the true position until this difference is below a certain threshold. We encourage you to check the*    ``WeightedLeastSquares``  *class to see how this is handled.*
