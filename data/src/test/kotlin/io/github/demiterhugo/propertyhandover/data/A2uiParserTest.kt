package io.github.demiterhugo.propertyhandover.data

import io.github.demiterhugo.propertyhandover.domain.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiParserTest {
    @Test
    fun `bundled messages define different controls and value types`() {
        val forms = LocalA2uiForms()
        val damage = forms.load(EntryType.DAMAGE)
        val meter = forms.load(EntryType.METER)

        val damageControls = (damage.root as A2uiNode.Column).children
        val meterControls = (meter.root as A2uiNode.Column).children
        assertEquals(listOf("room", "description", "submit"), damageControls.map { it.id })
        assertEquals(listOf("utility", "reading", "submit"), meterControls.map { it.id })
        val description = damageControls[1] as A2uiNode.TextField
        val utility = meterControls[0] as A2uiNode.ChoicePicker
        val reading = meterControls[1] as A2uiNode.TextField
        assertEquals("Descripción", description.label)
        assertEquals(A2uiNode.TextField.Variant.LONG_TEXT, description.variant)
        assertEquals(listOf("water", "electricity", "gas"), utility.options.map { it.value })
        assertEquals(InputValue.Choices(emptyList()), meter.initialValues[utility.path])
        assertEquals(A2uiNode.TextField.Variant.NUMBER, reading.variant)
        assertEquals("showSummary", (meterControls[2] as A2uiNode.Button).event)
    }

    @Test
    fun `changing only the message changes the render tree`() {
        val changedMessage = damageMessage()
            .replace("Estancia", "Zona")
            .replace("\"room\", \"description\", \"submit\"", "\"description\", \"room\", \"submit\"")
        val root = A2uiParser.parse(changedMessage).root as A2uiNode.Column
        assertEquals(listOf("description", "room", "submit"), root.children.map { it.id })
        assertEquals("Zona", (root.children[1] as A2uiNode.TextField).label)
    }

    @Test
    fun `malformed or trailing JSON is rejected`() {
        for (message in listOf("{", "{'version':'1.0'}", damageMessage() + "{}")) {
            assertThrows(A2uiException.Invalid::class.java) { A2uiParser.parse(message) }
        }
    }

    @Test
    fun `invalid references and data types are rejected before rendering`() {
        val message = damageMessage()
        val invalidMessages = listOf(
            message.replace("\"room\", \"description\"", "\"missing\", \"description\""),
            message.replace("\"room\", \"description\"", "\"root\", \"description\""),
            message.replace("\"id\": \"description\"", "\"id\": \"room\""),
            message.replace("\"room\": \"\"", "\"room\": 42"),
        )
        for (invalid in invalidMessages) {
            assertThrows(A2uiException.Invalid::class.java) { A2uiParser.parse(invalid) }
        }
    }

    @Test
    fun `unsupported component or protocol version is reported distinctly`() {
        val message = damageMessage()
        val unsupportedComponent = message.replace("\"component\": \"TextField\"", "\"component\": \"Image\"")
        assertEquals(
            "Component: Image",
            assertThrows(A2uiException.Unsupported::class.java) { A2uiParser.parse(unsupportedComponent) }.message,
        )
        assertThrows(A2uiException.Unsupported::class.java) {
            A2uiParser.parse(message.replace("\"version\": \"1.0\"", "\"version\": \"v0.9\""))
        }
    }

    private fun damageMessage(): String =
        checkNotNull(javaClass.getResourceAsStream("/a2ui/damage.json")).bufferedReader().use { it.readText() }
}
