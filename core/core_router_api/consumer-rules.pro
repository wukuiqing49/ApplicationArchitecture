# router_api shared keep rules
# Keep annotations and generic signatures because route tables and reflection depend on them.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Route annotations are consumed by the processor and may also be inspected at runtime.
-keep class com.wkq.router.annotation.** { *; }

# Keep generated route bootstrap classes.
-keep class com.wkq.router.generated.** { *; }

# @Param injection uses targetClassName + "_Syringe" to find the generated injector.
# Keep target class names when they contain @Param fields, otherwise R8 may rename
# the Activity/Fragment class and break Class.forName(target.javaClass.name + "_Syringe").
-keepclasseswithmembers class * {
    @com.wkq.router.annotation.Param <fields>;
}

# Keep SPI-style interfaces and their generated/reflectively-created implementations.
-keep public interface com.wkq.router.api.IRouteInit { *; }
-keep class * implements com.wkq.router.api.IRouteInit {
    public <init>();
}

-keep public interface com.wkq.router.api.IRouteGroup { *; }
-keep class * implements com.wkq.router.api.IRouteGroup {
    public <init>();
}

-keep public interface com.wkq.router.api.ISyringe { *; }
-keep class * implements com.wkq.router.api.ISyringe {
    public <init>();
}

-keep class * implements com.wkq.router.api.IDegradationService {
    public <init>();
}

# Keep route metadata used in the runtime route table.
-keep class com.wkq.router.api.RouteMeta { *; }
