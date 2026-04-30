# core_network shared keep rules
# Retrofit uses annotations and generic signatures at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Keep the common response wrapper and DTOs used by Gson/Retrofit examples.
-keep class com.wkq.net.BaseResponse { *; }
-keep class com.wkq.net.CloudFileInfo { *; }

# Keep sealed response types and nested states used by the network helpers.
-keep class com.wkq.net.core.ApiResponse { *; }
-keep class com.wkq.net.core.ApiResponse$Success { *; }
-keep class com.wkq.net.core.ApiResponse$Error { *; }
-keep class com.wkq.net.core.DownloadState { *; }
-keep class com.wkq.net.core.DownloadState$Progress { *; }
-keep class com.wkq.net.core.DownloadState$Success { *; }
-keep class com.wkq.net.core.DownloadState$Error { *; }

# Keep Retrofit service interfaces declared in this module.
-keep interface com.wkq.net.UploadService { *; }
-keep interface com.wkq.net.ApiService { *; }
-keep interface com.wkq.net.DownloadService { *; }
-keep interface com.wkq.net.RetrofitHelperExample$ThirdPartyService { *; }
