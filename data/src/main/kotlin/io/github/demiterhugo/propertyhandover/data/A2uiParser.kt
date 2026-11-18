package io.github.demiterhugo.propertyhandover.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException

internal object A2uiParser {
    private const val VERSION = "1.0"
    private const val BASIC_CATALOG = "https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json"
    private val topLevelBinding = Regex("/[A-Za-z][A-Za-z0-9_]*")

    fun parse(message: String): A2uiSurface {
        val envelope = parseJson(message).asObject("message")
        if (envelope.string("version") != VERSION) unsupported("Protocol version")
        envelope.allowOnly("version", "createSurface")

        val surface = envelope.obj("createSurface")
        surface.allowOnly("surfaceId", "catalogId", "components", "dataModel")
        if (surface.string("catalogId") != BASIC_CATALOG) unsupported("Component catalog")
        val surfaceId = surface.string("surfaceId")
        if (surfaceId.isBlank()) invalid("Empty surface ID")

        val components = mutableMapOf<String, JsonObject>()
        for (entry in surface.array("components")) {
            val component = entry.asObject("component")
            val id = component.string("id")
            if (id.isBlank() || components.put(id, component) != null) invalid("Invalid component ID: $id")
        }

        val reader = SurfaceReader(components, surface.obj("dataModel"))
        val root = reader.readNode("root")
        if (components.isNotEmpty()) invalid("Unreachable components: ${components.keys}")
        return A2uiSurface(surfaceId, root, reader.values.toMap())
    }

    private class SurfaceReader(
        private val remaining: MutableMap<String, JsonObject>,
        private val model: JsonObject,
    ) {
        val values = mutableMapOf<String, InputValue>()

        fun readNode(id: String): A2uiNode {
            val node = remaining.remove(id) ?: invalid("Missing or repeated component reference: $id")
            return when (val component = node.string("component")) {
                "Column" -> {
                    node.allowOnly("id", "component", "children")
                    A2uiNode.Column(id, node.array("children").map { readNode(it.asString("child ID")) })
                }
                "Card" -> {
                    node.allowOnly("id", "component", "child")
                    A2uiNode.Card(id, readNode(node.string("child")))
                }
                "Text" -> {
                    node.allowOnly("id", "component", "text")
                    A2uiNode.Text(id, node.string("text"))
                }
                "TextField" -> readTextField(id, node)
                "ChoicePicker" -> readChoicePicker(id, node)
                "Button" -> readButton(id, node)
                else -> unsupported("Component: $component")
            }
        }

        private fun readTextField(id: String, node: JsonObject): A2uiNode.TextField {
            node.allowOnly("id", "component", "label", "value", "variant", "placeholder")
            val path = node.binding()
            val variant = when (val value = node.get("variant")?.asString("variant") ?: "shortText") {
                "shortText" -> A2uiNode.TextField.Variant.SHORT_TEXT
                "longText" -> A2uiNode.TextField.Variant.LONG_TEXT
                "number" -> A2uiNode.TextField.Variant.NUMBER
                else -> unsupported("TextField variant: $value")
            }
            store(path, InputValue.Text(model.string(path.drop(1))))
            return A2uiNode.TextField(id, node.string("label"), path, variant, node.get("placeholder")?.asString("placeholder"))
        }

        private fun readChoicePicker(id: String, node: JsonObject): A2uiNode.ChoicePicker {
            node.allowOnly("id", "component", "label", "value", "variant", "displayStyle", "options")
            if (node.string("variant") != "mutuallyExclusive" || node.string("displayStyle") != "chips") {
                unsupported("ChoicePicker layout")
            }
            val options = node.array("options").map {
                val option = it.asObject("option")
                option.allowOnly("label", "value")
                A2uiNode.ChoicePicker.Option(option.string("label"), option.string("value"))
            }
            val allowedValues = options.map { it.value }.toSet()
            if (allowedValues.isEmpty() || allowedValues.size != options.size) invalid("Invalid choice options")
            val path = node.binding()
            val selected = model.array(path.drop(1)).map { it.asString("choice value") }
            if (selected.size > 1 || !allowedValues.containsAll(selected)) invalid("Invalid initial choice: $path")
            store(path, InputValue.Choices(selected))
            return A2uiNode.ChoicePicker(id, node.string("label"), path, options)
        }

        private fun readButton(id: String, node: JsonObject): A2uiNode.Button {
            node.allowOnly("id", "component", "child", "variant", "action")
            if (node.get("variant")?.asString("variant")?.let { it != "primary" } == true) {
                unsupported("Button variant")
            }
            val action = node.obj("action")
            action.allowOnly("event")
            val event = action.obj("event")
            event.allowOnly("name")
            val name = event.string("name")
            if (name.isBlank()) invalid("Empty event name")
            val child = readNode(node.string("child")) as? A2uiNode.Text ?: unsupported("Button child must be Text")
            return A2uiNode.Button(id, child, name)
        }

        private fun JsonObject.binding(): String {
            val binding = obj("value")
            binding.allowOnly("@path")
            val path = binding.string("@path")
            if (!topLevelBinding.matches(path)) unsupported("Binding path: $path")
            return path
        }

        private fun store(path: String, value: InputValue) {
            if (values.put(path, value) != null) unsupported("Repeated binding: $path")
        }
    }

    private fun parseJson(message: String): JsonElement = try {
        JsonReader(message.reader()).use { reader ->
            reader.strictness = Strictness.STRICT
            val json = JsonParser.parseReader(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) invalid("Trailing content in A2UI message")
            json
        }
    } catch (error: JsonParseException) {
        throw A2uiException.Invalid("Malformed A2UI JSON", error)
    } catch (error: IOException) {
        throw A2uiException.Invalid("Malformed A2UI JSON", error)
    }

    private fun JsonObject.allowOnly(vararg fields: String) {
        val unexpected = keySet() - fields.toSet()
        if (unexpected.isNotEmpty()) unsupported("Properties: $unexpected")
    }

    private fun JsonObject.required(key: String): JsonElement = get(key) ?: invalid("Missing $key")
    private fun JsonObject.obj(key: String) = required(key).asObject(key)
    private fun JsonObject.string(key: String) = required(key).asString(key)
    private fun JsonObject.array(key: String) = required(key).let {
        if (it.isJsonArray) it.asJsonArray else invalid("Expected array: $key")
    }

    private fun JsonElement.asObject(label: String): JsonObject =
        if (isJsonObject) asJsonObject else invalid("Expected object: $label")

    private fun JsonElement.asString(label: String): String =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else invalid("Expected string: $label")

    private fun invalid(message: String): Nothing = throw A2uiException.Invalid(message)
    private fun unsupported(message: String): Nothing = throw A2uiException.Unsupported(message)
}
