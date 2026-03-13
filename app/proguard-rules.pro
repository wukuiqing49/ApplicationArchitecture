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
# 自定义 Router 路由框架混淆规则
# ==============================================================================

# 1. 保护 Router 核心实现类，防止其 public 方法被混淆或移除
-keep object com.wkq.core.router.Router { *; }
-keep class com.wkq.core.router.RouteEntry { *; }
-keep class com.wkq.core.router.RouteEntry$* { *; }

# 2. 保护自动生成的 RouterInit 类（反射调用入口）
# 必须保持包名、类名、INSTANCE 实例以及 registerAll 方法，保证反射链路通畅
-keep class **.core.router.RouterInit {
    public static ** INSTANCE;
    public void registerAll();
}

# 3. 保护各模块定义的 *Routes 静态路由列表文件
-keep class **.*Routes {
    <fields>;
    <methods>;
}

# 4. 保护被引用的 Activity 和 Fragment 类名（防止 Intent 跳转因类名混淆报错）
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
