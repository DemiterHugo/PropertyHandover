package io.github.demiterhugo.propertyhandover.domain

enum class EntryType { DAMAGE, METER }

enum class HandoverField { ROOM, DESCRIPTION, UTILITY, READING }

const val DAMAGE_DESCRIPTION_LIMIT = 240

enum class FieldError(val message: String) {
    REQUIRED("Este campo es obligatorio."),
    ROOM_REQUIRED("Indica la estancia."),
    DESCRIPTION_REQUIRED("Describe el daño."),
    DESCRIPTION_TOO_LONG("Reduce la descripción a $DAMAGE_DESCRIPTION_LIMIT caracteres."),
    INVALID_UTILITY("Elige un tipo de suministro."),
    INVALID_READING("Introduce una lectura igual o mayor que cero."),
}

sealed interface HandoverResult {
    data class Invalid(val errors: Map<HandoverField, FieldError>) : HandoverResult
    data class Summary(val text: String) : HandoverResult
}

class Handover {
    fun summarize(type: EntryType, values: Map<HandoverField, String>): HandoverResult = when (type) {
        EntryType.DAMAGE -> summarizeDamage(
            room = values[HandoverField.ROOM].orEmpty().trim(),
            description = values[HandoverField.DESCRIPTION].orEmpty().trim(),
        )
        EntryType.METER -> summarizeMeter(
            utility = values[HandoverField.UTILITY].orEmpty(),
            reading = values[HandoverField.READING].orEmpty().trim(),
        )
    }

    private fun summarizeDamage(room: String, description: String): HandoverResult {
        val errors = buildMap {
            if (room.isBlank()) put(HandoverField.ROOM, FieldError.ROOM_REQUIRED)
            when {
                description.isBlank() -> put(HandoverField.DESCRIPTION, FieldError.DESCRIPTION_REQUIRED)
                description.length > DAMAGE_DESCRIPTION_LIMIT -> put(HandoverField.DESCRIPTION, FieldError.DESCRIPTION_TOO_LONG)
            }
        }
        if (errors.isNotEmpty()) return HandoverResult.Invalid(errors)

        return HandoverResult.Summary("Daño en la vivienda\nEstancia: $room\nDescripción: $description")
    }

    private fun summarizeMeter(utility: String, reading: String): HandoverResult {
        val utilityLabel = utilityLabels[utility]
        val decimal = reading.takeIf(readingPattern::matches)?.replace(',', '.')?.toBigDecimalOrNull()
        val errors = buildMap {
            if (utilityLabel == null) put(HandoverField.UTILITY, FieldError.INVALID_UTILITY)
            when {
                reading.isBlank() -> put(HandoverField.READING, FieldError.REQUIRED)
                decimal == null -> put(HandoverField.READING, FieldError.INVALID_READING)
            }
        }
        if (errors.isNotEmpty()) return HandoverResult.Invalid(errors)

        val formattedReading = checkNotNull(decimal).toPlainString().replace('.', ',')
        return HandoverResult.Summary("Lectura de contador\nSuministro: $utilityLabel\nLectura: $formattedReading")
    }

    private companion object {
        val utilityLabels = mapOf("water" to "Agua", "electricity" to "Electricidad", "gas" to "Gas")
        val readingPattern = Regex("[0-9]+([,.][0-9]+)?")
    }
}
