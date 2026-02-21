# Anime Draw AI - ProGuard Rules for Release Build

# Keep source file and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signature for Kotlin
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# ===== Retrofit =====
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Keep Retrofit annotations
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep API interfaces
-keep interface com.doyouone.drawai.data.api.** { *; }
-keepclassmembers interface com.doyouone.drawai.data.api.** {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit Call, Response, and Callback
-keep class retrofit2.Call { *; }
-keep class retrofit2.Response { *; }
-keep class retrofit2.Callback { *; }

# Keep Retrofit Converter
-keep class retrofit2.converter.gson.GsonConverterFactory { *; }
-keep class retrofit2.converter.** { *; }

# Keep API service methods
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit generic signatures
-keepattributes Signature
-keep class kotlin.coroutines.Continuation

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# ===== Gson =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn sun.misc.**

# Keep ALL Gson classes and internal classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.** { *; }
-keep class com.google.gson.internal.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep TypeToken and generic type information
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    *;
}

# Keep fields with SerializedName - DO NOT obfuscate
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep ALL generic signatures for reflection
-keep class * implements java.lang.reflect.ParameterizedType { *; }
-keep class * implements java.lang.reflect.GenericArrayType { *; }
-keep class * implements java.lang.reflect.Type { *; }
-keep class * implements java.lang.reflect.TypeVariable { *; }
-keep class * implements java.lang.reflect.WildcardType { *; }

# Keep reflection methods
-keepclassmembers class * {
    java.lang.reflect.Type getGenericSuperclass();
    java.lang.reflect.Type[] getGenericInterfaces();
}

# Prevent stripping of generic type information
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.Unsafe

# Keep $Gson$Types and $Gson$Preconditions
-keep class com.google.gson.internal.$Gson$Types { *; }
-keep class com.google.gson.internal.$Gson$Preconditions { *; }

# ===== Data Classes =====
# Keep ALL data classes without optimization or obfuscation
-keep,allowoptimization class com.doyouone.drawai.data.** { *; }
-keep,allowoptimization class com.doyouone.drawai.data.model.** { *; }
-keep,allowoptimization class com.doyouone.drawai.data.api.** { *; }

# Keep all model class fields and constructors - CRITICAL for Gson
-keepclassmembers class com.doyouone.drawai.data.model.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Specifically keep API response models - NO optimization
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.ApiResponse { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.WorkflowInfo { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.WorkflowsResponse { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.GenerateRequest { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.GenerateResponse { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.TaskStatusResponse { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.HealthResponse { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.Workflow { *; }
-keep,allowshrinking,allowobfuscation class com.doyouone.drawai.data.model.GeneratedImage { *; }

# Keep ALL members of these classes
-keepclassmembers class com.doyouone.drawai.data.model.ApiResponse { *; }
-keepclassmembers class com.doyouone.drawai.data.model.GenerateResponse { *; }
-keepclassmembers class com.doyouone.drawai.data.model.TaskStatusResponse { *; }
-keepclassmembers class com.doyouone.drawai.data.model.WorkflowsResponse { *; }

# Disable optimization for model classes to preserve generic types
-optimizations !class/unboxing/enum,!code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firestore model classes
-keepclassmembers class com.doyouone.drawai.data.repository.** {
  <fields>;
  <methods>;
}

# ===== Google AdMob =====
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep class com.google.android.gms.ads.identifier.** { *; }
-dontwarn com.google.android.gms.ads.**

# ===== Jetpack Compose =====
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose UI classes
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===== Coil (Image Loading) =====
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ===== DataStore =====
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    <fields>;
}

# ===== Lifecycle =====
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# ===== Navigation =====
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

# ===== ViewModel =====
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ===== Parcelable =====
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ===== Serializable =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== WorkManager =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.** { *; }

# ===== Google Sign-In =====
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ===== General Android =====
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ===== Enums =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== App Specific - UI Screens =====
# Keep all Composable functions
-keep class com.doyouone.drawai.ui.screens.** { *; }
-keepclassmembers class com.doyouone.drawai.ui.screens.** {
    <methods>;
}

# Keep AuthManager and all auth-related classes
-keep class com.doyouone.drawai.auth.** { *; }
-keepclassmembers class com.doyouone.drawai.auth.** {
    <methods>;
}

# Keep ViewModel classes and their methods
-keep class com.doyouone.drawai.ui.viewmodel.** { *; }
-keepclassmembers class com.doyouone.drawai.ui.viewmodel.** {
    <methods>;
}

# Keep Repository classes and all methods
-keep class com.doyouone.drawai.data.repository.** { *; }
-keepclassmembers class com.doyouone.drawai.data.repository.** {
    <methods>;
    <fields>;
}

# Specifically keep DrawAIRepository methods
-keep class com.doyouone.drawai.data.repository.DrawAIRepository {
    public <methods>;
    private <methods>;
}
-keepclassmembers class com.doyouone.drawai.data.repository.DrawAIRepository {
    *** getWorkflows(...);
    *** getDummyWorkflowsMap(...);
    *** generateImage(...);
}

# Keep API Service interfaces
-keep interface com.doyouone.drawai.data.api.** { *; }
-keepclassmembers interface com.doyouone.drawai.data.api.** {
    <methods>;
}

# Keep all lambda functions in UI
-keepclassmembers class com.doyouone.drawai.** {
    *** onGuestLogin(...);
    *** onLoginSuccess(...);
    *** onGoogleLogin(...);
}

# Keep Kotlin sealed classes
-keep class com.doyouone.drawai.data.model.** { *; }
-keepclassmembers class com.doyouone.drawai.data.model.** {
    <fields>;
    <methods>;
}

# ===== App Specific - Essential Classes Only =====
# Keep ONLY essential entry point classes (enable obfuscation for the rest)
-keep class com.doyouone.drawai.MainActivity { *; }
-keep class com.doyouone.drawai.DrawAIApplication { *; }

# Keep data models (Gson/Firebase needs them)
-keep class com.doyouone.drawai.data.model.** { *; }

# Keep API interfaces (Retrofit needs them)
-keep interface com.doyouone.drawai.data.api.** { *; }

# Everything else (UI, ViewModels, Repository, etc.) will be OBFUSCATED ✓
# This makes reverse engineering much harder

# Keep all Kotlin metadata
-keep class kotlin.Metadata { *; }

# ===== R8 Full Mode =====
# Temporarily disable aggressive optimization to debug release issues
# -allowaccessmodification
# -repackageclasses