# Exif & Imaging libraries
-dontwarn org.apache.commons.imaging.**
-dontwarn com.drew.**

# MetadataTemplate is persisted via Gson
-keep class com.wkq.iptc.feature.press.model.** { *; }
-keep class com.wkq.iptc.feature.press.metadata.MetadataVerificationResult { *; }
-keep class com.wkq.util.exif.ImageMetadataInspector$MetadataSummary { *; }
-keepattributes Signature
-keepattributes *Annotation*
