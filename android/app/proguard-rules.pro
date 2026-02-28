# ============================================================
# ChromaDMX R8 / ProGuard Rules
# ============================================================
# Deliberately broad for v1 — correctness over APK size.
# Tighten rules once release builds are stable.

# --- App classes (Koin uses reflection for DI) ---
-keep class com.chromadmx.** { *; }

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

# --- Ktor / OkHttp ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# --- Koog AI agent ---
-keep class ai.koog.** { *; }
-dontwarn ai.koog.**

# --- SQLDelight ---
-keep class app.cash.sqldelight.** { *; }
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
-keep class io.netty.** { *; }

# --- General ---
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
