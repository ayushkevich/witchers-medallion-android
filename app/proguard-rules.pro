# BLE - preserve Bluetooth classes
-keep class android.bluetooth.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.internal.** {
    volatile *;
}
