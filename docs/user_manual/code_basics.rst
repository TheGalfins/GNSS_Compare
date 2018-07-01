Getting started with the code
=============================


Importing the project to Android Studio
---------------------------------------

It's not necessary to use `Android Studio`_ to make modifications and build code, there are many other IDE's for Android development. It has been our choice so far, so we will be focusing our tutorials on that IDE at the moment.



Using the Google Maps Viewer
----------------------------

In order to use the Google Maps Viewer in GNSS Comapre, you'll need to get your own Google Maps API key and paste it into the Android manifest.

To get the Google Maps SDK key, follow this `guide`_.

After you have the key (it should start with ``AIza``), copy it and paste to the ``AndroidManifest.xml``. If you're using Android Studio, you can find ``AndroidManifest.xml`` by double-tapping shift and typing the name of the file. Find this line in the manifest file:

.. code-block:: xml

  <meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />


And replace ``"YOUR_API_KEY"`` with your API key.



.. _`Android Studio`: https://developer.android.com/studio/
.. _`guide`: https://developers.google.com/maps/documentation/android-sdk/signup
