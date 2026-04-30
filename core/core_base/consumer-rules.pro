# core_base shared keep rules
# Keep generic metadata so reflection on generic superclass continues to work in release builds.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Preserve generated ViewBinding classes and their members because BaseActivity/BaseFragment
# resolve the binding type by reflection.
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Preserve the shared base ViewModel and all Activity/Fragment subclasses used by the app.
-keep class com.wkq.base.BaseViewModel { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends androidx.fragment.app.Fragment { *; }
