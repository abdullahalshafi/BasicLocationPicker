# Keep all public types of the library (models passed via Intent + entry points)
-keep class com.shafi.basic_location_picker.** { *; }

# Keep Parcelable CREATOR fields (required by typed getParcelableExtra on API 33+)
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep enum members (LocationFailReason passed via .name / valueOf)
-keepclassmembers enum com.shafi.basic_location_picker.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
