# The bundled JSON (books, Korean helper, passport, map, holidays, i18n) and
# voice packs are read into these classes by kotlinx-serialization; keep their
# generated serializers and the names the JSON uses.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class uz.haksenguz.ertaklar.data.models.**$$serializer { *; }
-keepclassmembers class uz.haksenguz.ertaklar.data.models.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class uz.haksenguz.ertaklar.** {
    <fields>;
}

# Navigation's type-safe routes are found by their serializers too.
-keep,includedescriptorclasses class uz.haksenguz.ertaklar.navigation.**$$serializer { *; }
-keepclassmembers class uz.haksenguz.ertaklar.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
