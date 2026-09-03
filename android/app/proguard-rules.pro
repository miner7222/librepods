# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class me.kavishdevar.librepods.utils.KotlinModule { *; }

# NativeBridge is bound by name from l2c_fcr_hook.cpp
# (Java_me_kavishdevar_librepods_utils_NativeBridge_setSdpHook), and it is a
# separate top-level object, so the KotlinModule rule above does not cover it.
-keep class me.kavishdevar.librepods.utils.NativeBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
