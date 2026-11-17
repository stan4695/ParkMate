# Add project specific ProGuard rules here.
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.parkmate.app.**$$serializer { *; }
-keepclassmembers class com.parkmate.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.parkmate.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
