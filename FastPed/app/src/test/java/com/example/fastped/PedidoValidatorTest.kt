package com.example.fastped;

import org.junit.Test
import org.junit.Assert.*
import org.junit.Assert.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class PedidoValidatorTest {
    /*
    @Test
    fun horaEsValida() {
        val validator = PedidoValidator()
        assertTrue(validator.isHoraValida("2025-07-01T12:00:00"))
    }

    @Test
    fun horaNoEsValida() {
        val validator = PedidoValidator()
        assertFalse(validator.isHoraValida("1990-01-01T08:00:00"))
    }
    */
    private val validator = PedidoValidator()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @Test
    fun horaFuturaUnMinutoEsValida() {
        val future = LocalDateTime.now().plusMinutes(1).format(formatter)
        assertTrue("Una hora un minuto en el futuro debe ser válida",
            validator.isHoraValida(future))
    }

    @Test
    fun horaPasadaUnMinutoNoEsValida() {
        val past = LocalDateTime.now().minusMinutes(1).format(formatter)
        assertFalse("Una hora un minuto en el pasado debe ser inválida",
            validator.isHoraValida(past))
    }

    @Test
    fun formatoInvalidoDevuelveFalse() {
        // Cadenas no ISO o mal formateadas
        assertFalse(validator.isHoraValida("07/01/2025 12:00"))
        assertFalse(validator.isHoraValida("hoy a las doce"))
    }


}
