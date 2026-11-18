package io.github.demiterhugo.propertyhandover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.propertyhandover.data.A2uiNode
import io.github.demiterhugo.propertyhandover.data.InputValue

@Composable
internal fun A2uiForm(
    node: A2uiNode,
    values: Map<String, InputValue>,
    errors: Map<String, String>,
    onValueChange: (String, InputValue) -> Unit,
    onEvent: (String) -> Unit,
) {
    when (node) {
        is A2uiNode.Column -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (child in node.children) {
                key(child.id) { A2uiForm(child, values, errors, onValueChange, onEvent) }
            }
        }
        is A2uiNode.Text -> Text(node.text)
        is A2uiNode.TextField -> {
            val value = (values[node.path] as InputValue.Text).value
            val error = errors[node.path]
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(node.path, InputValue.Text(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(node.label) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = node.variant != A2uiNode.TextField.Variant.LONG_TEXT,
                minLines = if (node.variant == A2uiNode.TextField.Variant.LONG_TEXT) 3 else 1,
                maxLines = if (node.variant == A2uiNode.TextField.Variant.LONG_TEXT) 5 else 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (node.variant == A2uiNode.TextField.Variant.NUMBER) {
                        KeyboardType.Decimal
                    } else {
                        KeyboardType.Text
                    },
                ),
            )
        }
        is A2uiNode.ChoicePicker -> {
            val selected = (values[node.path] as InputValue.Choices).values
            Column {
                Text(node.label, style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (option in node.options) {
                        FilterChip(
                            selected = option.value in selected,
                            onClick = { onValueChange(node.path, InputValue.Choices(listOf(option.value))) },
                            label = { Text(option.label) },
                        )
                    }
                }
                errors[node.path]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        is A2uiNode.Button -> Button(onClick = { onEvent(node.event) }, modifier = Modifier.fillMaxWidth()) {
            A2uiForm(node.child, values, errors, onValueChange, onEvent)
        }
    }
}
