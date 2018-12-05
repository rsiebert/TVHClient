# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Program Files (x86)\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep        class android.support.v13.** { *; }
-keep        class android.support.v7.** { *; }
-keep        class android.support.v4.** { *; }

-dontwarn okhttp3.internal.platform.*

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Google play services
#-keep class com.google.android.gms.**
#-dontwarn com.google.android.gms.**

# Exoplayer
#-keep class com.google.android.exoplayer2.**
#-dontwarn com.google.android.exoplayer2.**

# Material dialogs
#-keep class com.afollestad.materialdialogs.**
#-dontwarn com.afollestad.materialdialogs.**

# Material drawer
#-keep class com.mikepenz.materialize.**
#-dontwarn com.mikepenz.materialize.**