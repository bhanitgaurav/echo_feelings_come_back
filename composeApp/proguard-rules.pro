# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.**
-dontwarn java.lang.management.**

# Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# App Specific (Keep Data Models just in case, though R8 is usually smart enough)
-keep class com.bhanit.apps.echo.data.model.** { *; }
-keep class com.bhanit.apps.echo.shared.data.model.** { *; }

# Keep ALL Serializable classes (Robust fix)
-keep @kotlinx.serialization.Serializable class * {
    *;
}
