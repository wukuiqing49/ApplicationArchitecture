# component_common shared keep rules
# Keep JavascriptInterface members because WebView calls them by name via reflection.
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
