#TVHClient

This android application allows you to interact and control the TVHeadend server. It is based on TVHGuide but contains many improvements and new features. It provides a clean and modern UI. The following major features are available: 

<b>Main features:</b>
* Show TV channels
* Show current and upcoming programs
* Full EPG (electronic program guide)
* Schedule and manage recordings
* Watch TV or your recordings
* Search for programs
* Modern and intuitive design
* Supports multiple TVHeadend servers
<br />

<b>Other features:</b>
* Filter channels by category
* Show server statistics
* Different playback profiles for TV programs and recordings
* Show program genre colors
* Multiple languages
* Light and dark theme

This program is licensed under the GPLv3 (see COPYING).

#How can I help?

If you are a programmer, fork the project, and provide patches or enhancements via pull requests. If you don't have coding skills, you can help with translations. Otherwise, file bugs, and open enhancement requests.

#Building from Source (Eclipse)

* Install Android SDK (http://developer.android.com/sdk/installing/index.html?pkg=tools)
* Install the Eclipse ADT bundle (http://stackoverflow.com/questions/27418096/where-can-i-download-eclipse-android-bundle)
* Clone the TVHClient repository
* Import TVHClient as existing project into Eclipse
* Import the android-support-v7-appcompat and android-design libraries from the SDK folder
* Download the required external libraries and import them into Eclipse
  * Material Dialogs (https://github.com/afollestad/material-dialogs)
  * Material Design (https://github.com/navasmdc/MaterialDesignLibrary)
  * datetimepicker (https://github.com/flavienlaurent/datetimepicker)
* Right click on TVHClient -> Properties -> Android and check if the five libraries are available
