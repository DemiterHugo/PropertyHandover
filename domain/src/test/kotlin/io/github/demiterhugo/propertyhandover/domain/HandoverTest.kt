package io.github.demiterhugo.propertyhandover.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HandoverTest {
    private val handover = Handover()

    @Test
    fun `damage requires both fields`() {
        assertEquals(
            HandoverResult.Invalid(
                mapOf(HandoverField.ROOM to FieldError.ROOM_REQUIRED, HandoverField.DESCRIPTION to FieldError.DESCRIPTION_REQUIRED),
            ),
            handover.summarize(EntryType.DAMAGE, mapOf(HandoverField.ROOM to "  ")),
        )
    }

    @Test
    fun `damage caps the description and trims the summary`() {
        val atLimit = "a".repeat(240)
        assertEquals(
            HandoverResult.Summary("Daño en la vivienda\nEstancia: Salón\nDescripción: $atLimit"),
            handover.summarize(EntryType.DAMAGE, mapOf(HandoverField.ROOM to "Salón", HandoverField.DESCRIPTION to " $atLimit ")),
        )
        assertEquals(
            HandoverResult.Invalid(mapOf(HandoverField.DESCRIPTION to FieldError.DESCRIPTION_TOO_LONG)),
            handover.summarize(EntryType.DAMAGE, mapOf(HandoverField.ROOM to "Salón", HandoverField.DESCRIPTION to "a".repeat(241))),
        )
        assertEquals(
            HandoverResult.Summary("Daño en la vivienda\nEstancia: Salón\nDescripción: Marca en la pared"),
            handover.summarize(
                EntryType.DAMAGE,
                mapOf(HandoverField.ROOM to " Salón ", HandoverField.DESCRIPTION to " Marca en la pared "),
            ),
        )
    }

    @Test
    fun `meter requires a recognized utility and a reading`() {
        assertEquals(
            HandoverResult.Invalid(
                mapOf(HandoverField.UTILITY to FieldError.INVALID_UTILITY, HandoverField.READING to FieldError.REQUIRED),
            ),
            handover.summarize(EntryType.METER, mapOf(HandoverField.UTILITY to "unknown")),
        )
    }

    @Test
    fun `meter accepts comma or dot and formats the summary in Spanish`() {
        for (reading in listOf("128,4", " 128.4 ")) {
            assertEquals(
                HandoverResult.Summary("Lectura de contador\nSuministro: Agua\nLectura: 128,4"),
                handover.summarize(EntryType.METER, mapOf(HandoverField.UTILITY to "water", HandoverField.READING to reading)),
            )
        }
        assertEquals(
            HandoverResult.Summary("Lectura de contador\nSuministro: Gas\nLectura: 0"),
            handover.summarize(EntryType.METER, mapOf(HandoverField.UTILITY to "gas", HandoverField.READING to "0")),
        )
    }

    @Test
    fun `meter rejects negative readings and ambiguous number formats`() {
        for (reading in listOf("-1", "1e4", "1.000,00", "128,", "NaN")) {
            assertEquals(
                reading,
                HandoverResult.Invalid(mapOf(HandoverField.READING to FieldError.INVALID_READING)),
                handover.summarize(EntryType.METER, mapOf(HandoverField.UTILITY to "water", HandoverField.READING to reading)),
            )
        }
    }
}
