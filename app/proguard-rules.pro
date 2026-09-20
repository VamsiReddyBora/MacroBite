# Disable obfuscation and aggressive optimization for 100% stability
-dontobfuscate
-dontoptimize

# Preserve all app code
-keep class com.macrobite.app.** { *; }

# Preserve Hilt & Dagger
-keep class dagger.** { *; }
-keep class * extends dagger.hilt.** { *; }
-keep interface dagger.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Preserve Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Preserve Gson models and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve DataStore & Coroutines
-keep class androidx.datastore.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
