# AMap Location SDK
-keep class com.amap.api.location.** { *; }
-keep class com.amap.api.fence.** { *; }
-keep class com.autonavi.aps.amapapi.model.** { *; }
-keep class com.autonavi.amap.mapcore.** { *; }
-keep class com.amap.api.maps.** { *; }
-keep class com.autonavi.amapapi.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# Room geo database and entities.
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class *Database_Impl { *; }
-keep class com.wkq.util.location.geo.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
