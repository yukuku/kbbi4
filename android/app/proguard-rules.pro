# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Developer/android-sdk-macosx/tools/proguard/proguard-android.txt
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

-dontwarn com.f2prateek.dart.internal.**
-keep class **$$ExtraInjector { *; }
-keepclasseswithmembernames class * {
    @com.f2prateek.dart.* <fields>;
}

# for dart 2.0 only
-keep class **Henson { *; }
-keep class **$$IntentBuilder { *; }

# remove can't find referenced class java.lang.invoke.LambdaForm$Hidden
-dontwarn java.lang.invoke.**

-dontobfuscate
