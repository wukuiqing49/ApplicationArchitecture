# Router X 核心库混淆规则
# 防止通过反射调用生成的类时失败

# 保持 IRouteInit 接口及其实现类
-keep public interface com.wkq.router.api.IRouteInit { *; }
-keep class com.wkq.router.generated.** implements com.wkq.router.api.IRouteInit {
    public <init>();
}

# 保持 IRouteGroup 接口及其实现类
-keep public interface com.wkq.router.api.IRouteGroup { *; }
-keep class com.wkq.router.generated.** implements com.wkq.router.api.IRouteGroup {
    public <init>();
}

# 保持 ISyringe 接口及其所有实现类（Syringe 类生成在业务包名下）
-keep public interface com.wkq.router.api.ISyringe { *; }
-keep class * implements com.wkq.router.api.ISyringe {
    public <init>();
}

# 保持业务服务的实现类（因为是通过反射实例化的）
-keep class * implements com.wkq.router.api.IDegradationService {
    public <init>();
}

# 保持 RouteMeta 数据类
-keep class com.wkq.router.api.RouteMeta { *; }

# 保持注解
-keepattributes *Annotation*
