# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                                                                           #
#                  MODERN ANDROID R8 / PROGUARD RULES                         #
#                                                                           #
# R8 is the default shrinker, optimizer, and obfuscator for Android. These  #
# rules work for both R8 and the older ProGuard.                            #
#                                                                           #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

# Keep debugging information for stack traces. This is highly recommended for
# production builds to help with debugging crashes from user reports.
-keepattributes SourceFile,LineNumberTable,Signature

# Keep Kotlin-specific metadata. This is ESSENTIAL for apps written in Kotlin
# to prevent crashes related to reflection and other runtime operations.
-keep,allowobfuscation,allowshrinking class kotlin.Metadata
-keep class * extends kotlin.coroutines.jvm.internal.SuspendLambda

# --- AndroidX Core Components ---
# Keep custom Views, which are often instantiated via reflection from XML layouts.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Application, Activity, Service, BroadcastReceiver, ContentProvider classes.
# The default Android rules often handle this, but being explicit can prevent issues.
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.lifecycle.ViewModel

# Keep classes that are used in native code (JNI).
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable creator fields.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep enums used in data binding or other reflection-based libraries.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# --- Optional: Common Libraries ---
# Uncomment the rules for the libraries you are using in your project.

# Retrofit & OkHttp
# -dontwarn retrofit2.**
# -keep class retrofit2.** { *; }
# -keepinterface retrofit2.** { *; }
# -keep class okhttp3.** { *; }
# -keep interface okhttp3.** { *; }
# -dontwarn okio.**
# -keep class okio.** { *; }


# Gson (if used for serialization with Retrofit or standalone)
# -keep class com.google.gson.stream.** { *; }
# -keep class * implements com.google.gson.TypeAdapterFactory
# -keep class * implements com.google.gson.JsonSerializer
# -keep class * implements com.google.gson.JsonDeserializer

# Moshi (if used for serialization)
# Keep classes annotated with @JsonClass to prevent obfuscation of adapter fields.
# -keep @com.squareup.moshi.JsonClass class * {
#   *;
# }

# Kotlinx Serialization
# -keepclassmembers class **$$serializer {
#   public static final ** INSTANCE;
# }

# WebView with a JavaScript Interface
# If your project uses WebView with JS, specify the fully qualified class name.
# -keepclassmembers class fqcn.of.javascript.interface.for.webview {
#    @android.webkit.JavascriptInterface <methods>;
# }