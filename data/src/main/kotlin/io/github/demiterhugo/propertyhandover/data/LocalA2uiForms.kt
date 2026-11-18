package io.github.demiterhugo.propertyhandover.data

import io.github.demiterhugo.propertyhandover.domain.EntryType
import java.io.IOException

class LocalA2uiForms {
    fun load(type: EntryType): A2uiSurface {
        val fileName = when (type) {
            EntryType.DAMAGE -> "damage.json"
            EntryType.METER -> "meter.json"
        }
        val resource = javaClass.getResourceAsStream("/a2ui/$fileName")
            ?: throw IOException("Missing A2UI resource: $fileName")
        return resource.bufferedReader(Charsets.UTF_8).use { A2uiParser.parse(it.readText()) }
    }
}
