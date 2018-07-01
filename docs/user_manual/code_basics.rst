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

After you have the key (it will look like a string of random characters, starting with ``AIza``), you'll have to make a new resource file. copy and paste it to the ``map_api_key.xml`` file located in the ``res/values`` directory. Find the lines containing the following:

.. code-block:: xml

  <string name="map_api_key">YOUR_API_KEY</string>

And replace ``YOUR_API_KEY`` with your API key.

Remember not to share the api code with anyone! If you're using git, you can mark that file as one which should not be tracked with

.. code-block:: bash

	git update-index --assume-unchanged app/src/main/res/values/map_api_key.xml

This way, the file will remain in your repository, but any changes made to it will be ignored, so e.g. your API key will not be pushed to the remote.



.. _`Android Studio`: https://developer.android.com/studio/
.. _`guide`: https://developers.google.com/maps/documentation/android-sdk/signup
