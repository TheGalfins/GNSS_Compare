<h1> GNSS Compare <a href='https://play.google.com/store/apps/details?id=com.galfins.gnss_compare&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width='20%' align='right'/></a> </h1>

Work in progress...

[![Build Status](https://travis-ci.org/TheGalfins/GNSS_Compare.svg?branch=master)](https://travis-ci.org/TheGalfins/GNSS_Compare) [![Documentation Status](https://readthedocs.org/projects/gnss-compare/badge/?version=latest)](https://gnss-compare.readthedocs.io/en/latest/?badge=latest) [![License](http://img.shields.io/:license-apache-blue.svg)](LICENSE.txt) [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)


_note: The L5/E5a frequency classes are in beta versions, so before using them please verify that the results are correct and within expected ranges._

Check out the [docs](https://gnss-compare.readthedocs.io).

Or dive straight into using the app. Now available on [Google Play Store](https://play.google.com/store/apps/details?id=com.galfins.gnss_compare).

The main ``app`` module of this work is released under the Apache 2.0 license.

The ``gogpsextracts`` module, is strongly based on the [goGPS Project](https://github.com/goGPS-Project/) and is released under the LGPL 3.0 license.

Todos:
- Refactor the code 
- Add tests
- Add [Google Play Scraper](https://github.com/facundoolano/google-play-scraper) or something similar to show app status from the Play store. 
- For IonoFree classes:
    - calculate correct C/N0 for signal strength
    - currently they're not returning the unused satellites properly


<sup>Google Play and the Google Play logo are trademarks of Google LLC.</sup>
