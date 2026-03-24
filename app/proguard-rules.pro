# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Administrator\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ==============================================================================
# 自定义 Router 路由框架混淆规则 (3 模块 KSP + SPI)
# ==============================================================================

# 1. 保护注解模块
-keep class com.wkq.router.annotation.** { *; }

# 2. 保护运行时核心实现类
-keep class com.wkq.router.api.** { *; }

# 3. 保护 KSP 自动生成的注册类 (RouteInit_XXX)
# 这些类由 ServiceLoader 反射实例化
-keep class com.wkq.router.generated.** { *; }

# 4. 保护 SPI 接口及其实现
-keep interface com.wkq.router.api.IRouteInit { *; }
-keep class * implements com.wkq.router.api.IRouteInit {
    public <init>();
}

# 5. 确保 META-INF/services 中的 SPI 配置文件不被移除或重命名
-keep class * implements com.wkq.router.api.IRouteInit { *; }

# 6. 保护被引用的 Activity 和 Fragment 类名 (防止 Intent 跳转报错)
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
