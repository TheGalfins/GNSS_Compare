
***********
User manual
***********

Let's start with the basics. GNSS compare allows the user to calculate the phone's location, based on the phone's GNSS pseudorange measurements. This is normally done by the phone automatically, however with the recent release of Android API 24 and the `GnssMeasurement`_ class, developers gained access to unprocessed pseudorange measurements. This can be used e.g., by GNSS scientists and researchers to come up with new, more precise or less resource intense methods for precise positioning. GNSS Compare is basically a tool for such scientists to compare their algorithms. And if you're not an GNSS expert, it can be a tool for you to learn more on this subject. And believe me -- there's a lot of interesting things to learn.

In order to fully understand what's under the hood for GNSS Compare, we must introduce a few terms we are using through the application. It's going to be a very general explanation -- if something will be not clear, please take a look at our :ref:`Glossary`. We hope you'll find answers to more detailed questions there. This section aims to provide a bridge between general GNSS knowledge, software practices, and GNSS Compare.


.. _`GnssMeasurement`: https://developer.android.com/reference/android/location/GnssMeasurement
