package io.github.demiterhugo.propertyhandover.data

data class A2uiSurface(
    val id: String,
    val root: A2uiNode,
    val initialValues: Map<String, InputValue>,
)

sealed interface InputValue {
    data class Text(val value: String) : InputValue
    data class Choices(val values: List<String>) : InputValue
}

sealed interface A2uiNode {
    val id: String

    data class Column(override val id: String, val children: List<A2uiNode>) : A2uiNode
    data class Text(override val id: String, val text: String) : A2uiNode
    data class TextField(
        override val id: String,
        val label: String,
        val path: String,
        val variant: Variant,
    ) : A2uiNode {
        enum class Variant { SHORT_TEXT, LONG_TEXT, NUMBER }
    }

    data class ChoicePicker(
        override val id: String,
        val label: String,
        val path: String,
        val options: List<Option>,
    ) : A2uiNode {
        data class Option(val label: String, val value: String)
    }

    data class Button(override val id: String, val child: Text, val event: String) : A2uiNode
}

sealed class A2uiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Invalid(message: String, cause: Throwable? = null) : A2uiException(message, cause)
    class Unsupported(message: String) : A2uiException(message)
}
