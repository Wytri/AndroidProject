package com.example.fastped

import org.junit.Assert.assertEquals
import com.example.fastped.model.PedidoProducto
import org.junit.Test

class PaymentUtilsTest {

    @Test
    fun calculateTotal_variosProductos_conDescuentos() {
        val lista = listOf(
            // precio 10.0, sin descuento, cantidad 2  → subtotal 20.0
            PedidoProducto("1", PrecioUnitario = 10.0, Cantidad = 2, Descuento = 0.0),
            // precio 20.0, 50% off, cantidad 1         → subtotal 10.0
            PedidoProducto("2", PrecioUnitario = 20.0, Cantidad = 1, Descuento = 50.0),
            // precio 5.25, sin descuento, cantidad 3    → subtotal 15.75
            PedidoProducto("3", PrecioUnitario = 5.25, Cantidad = 3, Descuento = null)
        )

        val total = PaymentUtils.calculateTotal(lista)
        // 20.00 + 10.00 + 15.75 = 45.75
        assertEquals(45.75, total, 0.0)
    }

    @Test
    fun calculateTotal_listaVacia_devuelveCero() {
        val total = PaymentUtils.calculateTotal(emptyList())
        assertEquals(0.0, total, 0.0)
    }
}
