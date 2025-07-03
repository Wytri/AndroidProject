package com.example.fastped.model

import com.google.firebase.Timestamp

/**
 * Representa un rol dentro de una tienda, creado por un dueño.
 * Ahora incluye permisos para distintas pantallas de la aplicación.
 */
data class Rol(
    val IDRol: String            = "",             // id de documento Firestore
    val storeId: String          = "",             // id de la tienda a la que pertenece
    val NombreRol: String        = "",             // nombre legible del rol
    val DescripcionRol: String   = "",             // descripción del rol
    val ColorHex: String         = "#000000",      // color asociado en hex
    val permissions: List<String> = emptyList(),     // pantallas a las que tiene acceso
    val createdBy: String        = "",             // dni (o uid) del dueño que lo creó
    val createdAt: Timestamp     = Timestamp.now()   // momento de creación
) {
    /** Convierte este objeto a Map para guardarlo en Firestore */
    fun toMap(): Map<String, Any> = mapOf(
        "NombreRol"      to NombreRol,
        "DescripcionRol" to DescripcionRol,
        "ColorHex"       to ColorHex,
        "permissions"    to permissions,
        "createdBy"      to createdBy,
        "createdAt"      to createdAt
    )
}