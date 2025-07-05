package com.example.fastped

import kotlin.collections.filter

data class Producto(val id: Int, val nombre: String, val disponible: Boolean)

object ProductUtils {
    /**
     * Filtra y devuelve solo los productos con disponible = true
     */
    fun filtrarDisponibles(lista: List<Producto>): List<Producto> {
        return lista.filter { it.disponible }
    }
}