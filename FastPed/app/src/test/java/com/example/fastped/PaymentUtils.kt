package com.example.fastped

import com.example.fastped.model.PedidoProducto

object PaymentUtils {
    /*
     Suma los subtotales de cada producto (aplica descuento y cantidad), deuelve el total en soles (Double).
     */
    fun calculateTotal(products: List<PedidoProducto>): Double {
        return products
            .sumOf { p ->
                val desc   = p.Descuento ?: 0.0
                val precio = p.PrecioUnitario * (1 - desc / 100.0)
                precio * p.Cantidad
            }
            .let { Math.round(it * 100) / 100.0 } // con este codigo, jefri redondea a 2 decimales
    }
}