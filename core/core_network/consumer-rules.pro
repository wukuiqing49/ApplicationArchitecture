# core_network shared keep rules
# Retrofit uses annotations and generic signatures at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, *Annotation*

# Retrofit creates implementations from annotated service interfaces at runtime.
-keepclasseswithmembers,allowshrinking,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep the common response wrapper.
-keep class com.wkq.net.BaseResponse { *; }

# Keep sealed response types and nested states used by the network helpers.
-keep class com.wkq.net.core.ApiResponse { *; }
-keep class com.wkq.net.core.ApiResponse$Success { *; }
-keep class com.wkq.net.core.ApiResponse$Error { *; }
-keep class com.wkq.net.core.ErrorType { *; }
-keep class com.wkq.net.core.DownloadState { *; }
-keep class com.wkq.net.core.DownloadState$Progress { *; }
-keep class com.wkq.net.core.DownloadState$Success { *; }
-keep class com.wkq.net.core.DownloadState$Error { *; }
-keep class com.wkq.net.core.UploadProgress { *; }
-keep class com.wkq.net.core.UploadProgressRequestBody { *; }
-keep class com.wkq.net.core.UploadProgressDetailRequestBody { *; }
-keep class com.wkq.net.core.UploadState { *; }
-keep class com.wkq.net.core.UploadState$Progress { *; }
-keep class com.wkq.net.core.UploadState$Success { *; }
-keep class com.wkq.net.core.UploadState$Error { *; }
-keep class com.wkq.net.core.Net { *; }
-keep interface com.wkq.net.core.NetResponseParser { *; }
-keep interface com.wkq.net.core.NetResponseParserFactory { *; }
-keep class com.wkq.net.core.BaseResponseParser { *; }
-keep class com.wkq.net.core.BaseResponseParserFactory { *; }
-keep class com.wkq.net.config.NetConfig { *; }
-keep class com.wkq.net.config.NetConfig$Builder { *; }

# Keep Retrofit service interfaces declared in this module.
-keep interface com.wkq.net.UploadService { *; }

# Gson DTO safety.
# Prefer @SerializedName on DTO fields. Common DTO/model packages are kept as fallback.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class **.model.** { *; }
-keep class **.models.** { *; }
-keep class **.bean.** { *; }
-keep class **.beans.** { *; }
-keep class **.dto.** { *; }
-keep class **.entity.** { *; }
-keep class **.response.** { *; }
-keep class **.request.** { *; }
