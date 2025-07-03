package com.example.fastped.model

data class Producto(
    val IDProducto: String = "",      // puedes darle valor por defecto
    val IDRes: String = "",
    val NombreProducto: String = "",
    val Descripcion: String = "",
    val Precio: Double = 0.0,
    val Descuento: Double = 0.0,
    val PhotoBase64: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "IDRes"           to IDRes,
        "NombreProducto"  to NombreProducto,
        "Descripcion"     to Descripcion,
        "Precio"          to Precio,
        "Descuento"       to Descuento,
        "PhotoBase64"     to PhotoBase64
    )
}