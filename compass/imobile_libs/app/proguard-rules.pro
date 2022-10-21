-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*


-keep public class * extends android.**
-keep public class * extends java.**

-keepclasseswithmembernames class * {
    void set*(***);
}

-keepclasseswithmembernames class * {
    void set*(int, ***);
}

#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}

#-keep class * implements android.os.Parcelable {
#  public static final android.os.Parcelable$Creator *;
#}

#过滤注解
#-keepattributes *Annotation*
#-keep class * extends java.lang.annotation.Annotation { *; }
#-keep interface * extends java.lang.annotation.Annotation { *; }
#过滤泛型
#-keepattributes Signature

#################################################
#-dontwarn sun.misc.**
#-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
#   long producerIndex;
#   long consumerIndex;
#}

#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode producerNode;
#}

#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode consumerNode;
#}

#-dontnote rx.internal.util.PlatformDependent

####################################################
#-keep public class * extends com.tbruyelle.**
-keep public class * extends com.readystatesoftware.sqliteasset.**
