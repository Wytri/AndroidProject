package com.example.fastped.model

/**
 * Representa al usuario completo que guardamos en Firestore y mostramos en UI.
 */

data class Usuario(
    val email: String,
    val dni: String,
    val nombre: String,
    val apellido: String,
    val celular: String,
    val tipo: Int,
    val sexo: Int,
    val pin: String,
    val photoBase64: String? = null,
    val tiendaId: String?   = null   // <— aquí
)