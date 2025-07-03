// UsuarioRestaurante.kt
package com.example.fastped.model

import com.google.firebase.Timestamp

/**
 * Representa la membresía de un usuario (trabajador) en una tienda,
 * junto con el rol y las fechas de validez.
 */
data class UsuarioRestaurante(
    val id: String = "",            // id del documento en Firestore
    val userId: String = "",        // dni del usuario
    val storeId: String = "",       // id de la tienda
    val roleId: String = "",        // id del rol asignado
    val joinCode: String = "",      // código único de la tienda usado para unirse
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null
) {
    /** Convierte a Map para almacenar en Firestore */
    fun toMap(): Map<String, Any?> = mapOf(
        "userId"     to userId,
        "storeId"    to storeId,
        "roleId"     to roleId,
        "joinCode"   to joinCode,
        "createdAt"  to createdAt,
        "expiresAt"  to expiresAt
    )
}