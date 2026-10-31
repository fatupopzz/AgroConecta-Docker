# Reglas de R8 para el build de release.
#
# Hilt, Room, WorkManager, OkHttp, Coil y Firebase traen sus propias reglas
# dentro de cada librería; aquí solo va lo que esas reglas no cubren.

# ── Modelos que viajan como JSON ────────────────────────────────────────────
# Gson llena los campos por nombre usando reflexión. Si R8 los renombra o los
# borra porque "nadie los lee", la app recibe objetos vacíos del servidor sin
# ningún error visible. Muchos modelos no llevan @SerializedName (el campo se
# llama igual que en el JSON), por eso se conserva el paquete completo.
-keep class com.uvg.agroconecta.data.models.** { *; }
-keep class com.uvg.agroconecta.ui.profile.FarmerProfile { *; }
-keep class com.uvg.agroconecta.ui.profile.DistributorProfile { *; }

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# El calendario guarda las siembras con un TypeToken<List<Planting>>; sin la
# firma genérica Gson no sabe de qué tipo es la lista al leer la caché.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ── Retrofit 2.9 ────────────────────────────────────────────────────────────
# Esta versión no trae reglas para el modo completo de R8 (el que usa AGP 8):
# sin ellas las funciones suspend que devuelven Response<T> fallan al primer
# llamado con un ClassCastException.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── LiteRT (TensorFlow Lite) ────────────────────────────────────────────────
# La librería nativa busca estas clases por nombre desde JNI; si R8 las
# renombra, el diagnóstico de hojas falla al cargar el modelo.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
