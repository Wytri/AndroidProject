package com.example.fastped

import com.example.fastped.ProductUtils
import com.example.fastped.model.Producto
import org.junit.Assert.*
import org.junit.Test

class ProductUtilsTest {

    @Test
    fun filtrarDisponibles_debeDejarSoloLosTrue() {
        val productos = listOf(
            Producto(1, "Lomo Saltado", true),
            Producto(2, "Ají de Gallina", false),
            Producto(3, "Ceviche", true)
        )

        val disponibles = ProductUtils.filtrarDisponibles(productos)

        // Comprueba que solo quedaron los dos con disponible = true
        assertEquals(2, disponibles.size)
        assertTrue(disponibles.all { it.disponible })
        assertEquals(listOf(1, 3), disponibles.map { it.id })
    }

    @Test
    fun filtrarDisponibles_siNoHayDisponibles_devuelveListaVacia() {
        val productos = listOf(
            Producto(1, "Papitas", false),
            Producto(2, "Choclo", false)
        )

        val disponibles = ProductUtils.filtrarDisponibles(productos)

        assertTrue(disponibles.isEmpty())
    }
}