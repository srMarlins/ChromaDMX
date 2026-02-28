# ============================================================
# ChromaDMX R8 / ProGuard Rules
# ============================================================

# --- Koin DI (reflection-based injection) ---
# Keep classes that Koin instantiates via reflection (modules, viewmodels)
-keep class com.chromadmx.**.di.* { *; }
-keep class com.chromadmx.**ViewModel { *; }
-keep class com.chromadmx.**ViewModel$* { *; }

# --- kotlinx-serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chromadmx.**$$serializer { *; }
-keepclassmembers class com.chromadmx.** {
    *** Companion;
}
-keepclasseswithmembers class com.chromadmx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor ---
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.server.engine.** { *; }
-keep class io.ktor.utils.io.** { *; }
-dontwarn io.ktor.**

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Koog AI agent (uses reflection for tool dispatch) ---
-keep class ai.koog.** { *; }
-dontwarn ai.koog.**

# --- SQLDelight ---
-keep class app.cash.sqldelight.driver.** { *; }
-dontwarn app.cash.sqldelight.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- CameraX ---
-keep class androidx.camera.** { *; }

# --- Netty (used by Ktor server) ---
-dontwarn io.netty.**

# --- General ---
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
