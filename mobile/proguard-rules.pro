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

-verbose

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }

-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.**
-dontwarn org.apache.lang.**
-dontwarn org.apache.commons.**
-dontwarn java.lang.invoke**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-keep class com.google.android.**  { *; }
-keep interface com.google.android.**  { *; }
-dontwarn com.google.android.**


-keep class org.apache.**  { *; }
-keep interface org.apache.**  { *; }


-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <fields>;
    @android.webkit.JavascriptInterface <methods>;
}