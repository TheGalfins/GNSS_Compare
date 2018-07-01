Getting started with the code
=============================


Importing the project to Android Studio
---------------------------------------

It's not necessary to use `Android Studio`_ to make modifications and build code, there are many other IDE's for Android development. It has been our choice so far, so we will be focusing our tutorials on that IDE at the moment.



.. Don't change the title below, as it is linked to the app's map_disabled_layout and map_disabled_description string resource!

Using the Google Maps Viewer
----------------------------

In order to use the Google Maps Viewer in GNSS Comapre, you'll need to get your own Google Maps API key and paste it into the Android manifest.

To get the Google Maps SDK key, follow this `guide`_.

After you have the key (it will look like a string of random characters, starting with ``AIza``), you'll have to copy and paste it to the ``AndroidManifest.xml`` file. If you're using Android Studio, you can find ``AndroidManifest.xml`` by double-tapping shift and typing the name of the file. Find the lines containing the following:

.. code-block:: xml

  <meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />


And replace ``YOUR_API_KEY`` with your API key (the quotation marks should remain).

Remember not to share the api code with anyone!



.. _`Android Studio`: https://developer.android.com/studio/
.. _`guide`: https://developers.google.com/maps/documentation/android-sdk/signup
