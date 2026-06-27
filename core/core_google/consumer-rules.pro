# Google Models (Auth, Billing, etc.)
-keep class com.wkq.google.model.** { *; }
-keep class com.wkq.google.billing.** { *; }
-keep class com.wkq.google.ads.GoogleReward { *; }
-keep class com.wkq.google.GoogleKitConfig { *; }
-keepclassmembers class com.wkq.google.model.** {
    <fields>;
}
-keepclassmembers class com.wkq.google.billing.** {
    <fields>;
}
-keepclassmembers class com.wkq.google.ads.GoogleReward {
    <fields>;
}
-keepattributes Signature
-keepattributes *Annotation*
