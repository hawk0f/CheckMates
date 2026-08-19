# kotlinx.serialization: keep generated serializers reachable via companion objects
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class dev.hawk0f.checkmates.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class dev.hawk0f.checkmates.**$$serializer {
    *** INSTANCE;
}

# Ktor uses java.lang.management reflectively on JVM only
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
