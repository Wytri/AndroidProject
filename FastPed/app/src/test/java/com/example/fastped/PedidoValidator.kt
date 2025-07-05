package com.example.fastped;

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

class PedidoValidator{
    /*
    fun isHoraValida(hora: String): Boolean {
        // Valida si el string contiene "20"
        return hora.contains("20")
    }*/
    fun isHoraValida(horaIso: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val fechaPedido = LocalDateTime.parse(horaIso, formatter)
            fechaPedido.isAfter(LocalDateTime.now())
        } catch (e: DateTimeParseException) {
            false  // Formato inválido
        }
    }
}
