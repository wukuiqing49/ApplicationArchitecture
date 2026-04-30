# core_util shared keep rules
# Preserve Parcelable creators so parcelable objects remain compatible in release builds.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
