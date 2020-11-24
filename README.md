# TVHClient

[![Debug Build Status](https://github.com/rsiebert/TVHClient/workflows/Debug%20Build/badge.svg)](https://github.com/rsiebert/TVHClient/actions)

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
* Sorting of channels
* Show channel logos
* Different playback profiles for TV programs and recordings
* Show program genre colors
* Multiple languages
* Light and dark theme
* Wake up the server via wake on LAN
* Show server statistics

This program is licensed under the GPLv3 (see LICENSE).

# Get in contact

Contact me either by
* creating a new issue in Github
* write a mail to rsiebert80@gmail.com
* Discord (https://discord.gg/RbBXfG3)

# How can I help?
    
* Provide pull request with patches or new features
* Report bugs and/or request new features
* Help with translations

# Building from Source (Android Studio)

* Download and install the latest Android Studio (http://developer.android.com/sdk/index.html)
* Clone the TVHClient repository within Android Studio
* Additionally the project requires the ExoPlayer library, so clone the repository (https://github.com/google/ExoPlayer) at the same level as TVHClient is located
* Open the project from Android Studio
* Note: The internal player uses the FFMpeg extension which needs to be compiled separately. To do so install MSYS2 from https://www.msys2.org/ in case you are running Windows. Then follow the instructions at https://github.com/google/ExoPlayer/tree/release-v2/extensions/ffmpeg

# Build Properties

Build customization can be performed via a `local-tvhclient.properties` file, for example:

    org.tvheadend.tvhclient.acraReportUri=https://crashreport.com/report/tvhclient
    org.tvheadend.tvhclient.keystoreFile=keystore.jks
    org.tvheadend.tvhclient.keystorePassword=MySecretPassword
    org.tvheadend.tvhclient.keyAlias=My TVHClient Key
    org.tvheadend.tvhclient.keyPassword=MySecretPassword
