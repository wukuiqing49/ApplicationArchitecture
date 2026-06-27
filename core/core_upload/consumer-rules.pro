# Optional server-side classes referenced by transitive upload libraries.
-dontwarn org.apache.commons.collections.map.LRUMap
-dontwarn org.apache.hadoop.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn sun.security.x509.X509Key

# SSH / SMB / crypto libraries used by upload protocols.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class jcifs.** { *; }
-dontwarn jcifs.**
-keep class net.schmizz.** { *; }
-dontwarn net.schmizz.**

# Retrofit / OkHttp service declarations.
-keep,allowshrinking,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Gson-persisted upload config models.
-keep class com.wkq.iptc.upload.UploadServerProfile { *; }
-keep class com.wkq.iptc.upload.UploadProtocolType { *; }
-keep class com.wkq.iptc.upload.UploadServerConfig { *; }
-keep class com.wkq.iptc.upload.UploadTask { *; }
-keep class com.wkq.iptc.upload.UploadFailure { *; }
-keep class com.wkq.iptc.upload.UploadErrorCode { *; }
-keep class com.wkq.iptc.upload.UploadResult { *; }
-keep class com.wkq.iptc.upload.UploadResult$* { *; }
-keep class com.wkq.iptc.upload.FtpConfig { *; }
-keep class com.wkq.iptc.upload.FtpsConfig { *; }
-keep class com.wkq.iptc.upload.SftpConfig { *; }
-keep class com.wkq.iptc.upload.HttpConfig { *; }
-keep class com.wkq.iptc.upload.SmbConfig { *; }
-keep class com.wkq.iptc.upload.WebDavConfig { *; }
-keep class com.wkq.iptc.upload.FtpsSecurityMode { *; }

