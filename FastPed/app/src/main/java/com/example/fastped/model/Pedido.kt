// app/src/main/java/com/example/fastped/model/Pedido.kt
package com.example.fastped.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Pedido(
    @get:PropertyName("idpedido") @PropertyName("idpedido")
    val idPedido: String = "",

    @get:PropertyName("idcliente") @PropertyName("idcliente")
    val idCliente: String = "",

    @get:PropertyName("medioDePago") @PropertyName("medioDePago")
    val medioDePago: String = "",

    @get:PropertyName("fechaCompra") @PropertyName("fechaCompra")
    val fechaCompra: Timestamp? = null,

    @get:PropertyName("estado") @PropertyName("estado")
    val estado: String = "",

    @get:PropertyName("total") @PropertyName("total")
    val total: Double = 0.0
)
