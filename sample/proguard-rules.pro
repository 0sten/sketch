# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
# 不混淆SearchView，因为发现混淆的时候会把SearchView的构造函数给弄丢了，导致无法实例化
-keep class android.support.v7.widget.SearchView { *; }

# 不混淆所有的枚举类，防止使用枚举类的名字来匹配时出问题
-keep enum * {*;}

##---------------Begin: proguard configuration for Gson  ----------
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *;}
-dontwarn com.google.gson.**
-keepattributes Exceptions, Signature, InnerClasses
##---------------End: proguard configuration for Gson  ----------

-keep public class com.tencent.bugly.**{*;}

##---------------Begain: Retrofit
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**
-dontwarn org.codehaus.mojo.**
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
##---------------End: Retrofit

##---------------Begain: FFmpegMediaMetadataRetriever
-keep public class wseemann.media.**{*;}
##---------------End: FFmpegMediaMetadataRetriever