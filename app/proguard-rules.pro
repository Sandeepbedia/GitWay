# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Git Way release R8 rules
#
# Deliberately narrow: we only keep the exact classes/members that are found
# through reflection at runtime (kotlinx.serialization's generated $$serializer
# companions, and the Retrofit service interface's method signatures/generic
# types). Everything else — Compose, Retrofit, OkHttp, AndroidX internals — is
# left to R8 and to the consumer-rules.pro each of those libraries already
# ships, so full shrinking/obfuscation/optimization still applies to them.
# ---------------------------------------------------------------------------

# Needed for Retrofit to inspect generic return types (Call<T>, etc.) and for
# kotlinx.serialization's reflection-based parts at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit service interface: keep its methods + annotations so Retrofit can
# build the HTTP request at runtime. Only this one interface, not all of
# Retrofit or all app classes.
-keep,allowobfuscation interface com.io.git.way.data.remote.GitHubApiService { *; }

# kotlinx.serialization: keep the generated $$serializer objects and the
# serializer() lookup for our own @Serializable DTOs/models only.
-keepclassmembers class com.io.git.way.** {
    *** Companion;
}
-keepclasseswithmembers class com.io.git.way.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.io.git.way.**$$serializer { *; }