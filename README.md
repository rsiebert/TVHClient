# TVHClient

[![Build Status](https://travis-ci.org/rsiebert/TVHClient.svg?branch=develop)](https://travis-ci.org/rsiebert/TVHClient)

This application allows you to fully control your TVHeadend server. 

<b>Main features:</b>
* Watch Live-TV or your recordings on your smartphone, tablet or Chromecast
* Show TV channels including current and upcoming programs
* Full electronic program guide
* Schedule and manage recordings
* Create series and timer recordings
* Search for programs and recordings
* Download recordings
* Modern and intuitive design
* Connect to multiple TVHeadend servers
<br />

<b>Other features:</b>
* Filter channels by channel tags
* Show channel logos
* Different playback profiles for TV programs and recordings
* Show program genre colors
* Multiple languages
* Light and dark theme
* Wake up the server via wake on LAN
* Show server statistics

This program is licensed under the GPLv3 (see LICENSE).

# How can I help?

You are welcome to help with translations, provide patches or add new features via pull requests. You can also report bugs and request new features.

# Building from Source (Android Studio)

* Download and install Android Studio (http://developer.android.com/sdk/index.html)
* Clone the TVHClient repository within Android Studio
* Open the project from Android Studio

# Build Properties

Build customization can be performed via a `local-tvhclient.properties` file, for example:

    org.tvheadend.tvhclient.acraReportUri=https://crashreport.com/report/tvhclient
    org.tvheadend.tvhclient.keystoreFile=keystore.jks
    org.tvheadend.tvhclient.keystorePassword=MySecretPassword
    org.tvheadend.tvhclient.keyAlias=My TVHClient Key
    org.tvheadend.tvhclient.keyPassword=MySecretPassword