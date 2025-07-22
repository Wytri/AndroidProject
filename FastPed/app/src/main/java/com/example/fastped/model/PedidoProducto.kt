package com.example.fastped.model

import com.google.firebase.firestore.PropertyName
import com.example.fastped.model.EstadosPedidoProducto

data class PedidoProducto(
    @get:PropertyName("IDPedido")    @PropertyName("IDPedido")
    val IDPedido: String = "",

    @get:PropertyName("IDRes")       @PropertyName("IDRes")
    val IDRes: String = "",

    @get:PropertyName("IDProducto")  @PropertyName("IDProducto")
    val IDProducto: String = "",

    @get:PropertyName("NombreProducto") @PropertyName("NombreProducto")
    val NombreProducto: String = "",

    @get:PropertyName("Descripcion")  @PropertyName("Descripcion")
    val Descripcion: String = "",

    @get:PropertyName("Cantidad")     @PropertyName("Cantidad")
    val Cantidad: Int = 1,

    @get:PropertyName("PrecioUnitario") @PropertyName("PrecioUnitario")
    val PrecioUnitario: Double = 0.0,

    @get:PropertyName("Descuento")    @PropertyName("Descuento")
    val Descuento: Double? = 0.0,

    @get:PropertyName("PhotoBase64")  @PropertyName("PhotoBase64")
    val PhotoBase64: String? = null,

    @get:PropertyName("Estado")       @PropertyName("Estado")
    val Estado: String = EstadosPedidoProducto.RECIBIDO
)
