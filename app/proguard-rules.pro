-keep class com.mimo.mobile.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.**
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
