// Restaurante.kt
package com.example.fastped.model

import com.google.firebase.Timestamp

data class Restaurante(
    val IDRes: String = "",                       // ← Valor por defecto
    val CodigoUnico: String? = null,
    val RUC: String = "",                   // ← NUEVO campo
    val Nombre: String = "",                      // ← Valor por defecto
    val Descripcion: String = "",                 // ← Valor por defecto
    val Direccion: String = "",                   // ← Valor por defecto
    val Distrito: String = "",                    // ← Valor por defecto
    val Provincia: String = "",                   // ← Valor por defecto
    val BannerBase64: String? = null,
    val StoreLogoBase64: String? = null,
    val createdBy: String = "",                   // ← dni del dueño
    val createdAt: Timestamp? = null              // ← Nullable
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "CodigoUnico"     to CodigoUnico,
        "RUC"             to RUC,            // ← incluimos RUC
        "Nombre"          to Nombre,
        "Descripcion"     to Descripcion,
        "Direccion"       to Direccion,
        "Distrito"        to Distrito,
        "Provincia"       to Provincia,
        "BannerBase64"    to BannerBase64,
        "StoreLogoBase64" to StoreLogoBase64,
        "createdBy"       to createdBy,
        "createdAt"       to createdAt
    )
}