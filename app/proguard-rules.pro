# TFLite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Room
-keep class com.recall.app.core.database.entity.** { *; }
-keep class com.recall.app.core.database.RecallDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Compose Navigation
-keep class * implements androidx.navigation.NavArgs { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin serialization (if used)
-keepattributes *Annotation*

# Coil
-dontwarn coil3.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
