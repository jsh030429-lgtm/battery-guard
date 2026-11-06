# Pleos SDK(Vehicle, TextToSpeech)는 리플렉션·AIDL·Gson을 쓰므로 그대로 보존
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-dontwarn ai.pleos.**

# SDK가 쓰는 Gson·Kotlin 리플렉션
-keep class com.google.gson.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# SDK 의존 라이브러리(JNA 등)
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-dontwarn java.awt.**
