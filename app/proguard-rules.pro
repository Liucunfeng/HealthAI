# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/replace/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep data model classes used for JSON (de)serialization
-keepclassmembers class com.example.healthai.vision.models.** { *; }
