package io.github.demiterhugo.propertyhandover.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.data.A2uiNode
import io.github.demiterhugo.propertyhandover.data.InputValue
import io.github.demiterhugo.propertyhandover.ui.theme.FormHeadingFont

@Composable
internal fun A2uiForm(
    node: A2uiNode,
    form: FormState.Ready,
    onValueChange: (String, InputValue) -> Unit,
    onEvent: (String) -> Unit,
) {
    when (node) {
        is A2uiNode.Column -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (child in node.children) {
                key(child.id) { A2uiForm(child, form, onValueChange, onEvent) }
            }
        }
        is A2uiNode.Card -> {
            val colors = MaterialTheme.colorScheme
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainerLow,
                contentColor = colors.onSurface,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Box(Modifier.padding(16.dp)) {
                    A2uiForm(node.child, form, onValueChange, onEvent)
                }
            }
        }
        is A2uiNode.Text -> {
            if (node.text.startsWith("## ") && '\n' !in node.text) {
                val colors = MaterialTheme.colorScheme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ) {
                    Text(
                        text = node.text.removePrefix("## "),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics { heading() },
                        fontFamily = FormHeadingFont,
                        fontSize = 30.sp,
                        lineHeight = 35.sp,
                    )
                }
            } else Text(node.text)
        }
        is A2uiNode.TextField -> A2uiTextField(node, form) { value ->
            onValueChange(node.path, InputValue.Text(value))
        }
        is A2uiNode.ChoicePicker -> A2uiChoicePicker(node, form) { value ->
            onValueChange(node.path, InputValue.Choices(listOf(value)))
        }
        is A2uiNode.Button -> Button(
            onClick = { onEvent(node.event) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(node.child.text, modifier = Modifier.weight(1f))
                Text("→", modifier = Modifier.clearAndSetSemantics {})
            }
        }
    }
}

@Composable
private fun A2uiChoicePicker(
    node: A2uiNode.ChoicePicker,
    form: FormState.Ready,
    onSelect: (String) -> Unit,
) {
    val selected = (form.values[node.path] as InputValue.Choices).values.singleOrNull()
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(node.label, style = MaterialTheme.typography.titleSmall)
        when (node.displayStyle) {
            A2uiNode.ChoicePicker.DisplayStyle.CHIPS -> FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (option in node.options) {
                    FilterChip(
                        selected = option.value == selected,
                        onClick = { onSelect(option.value) },
                        label = { Text(option.label) },
                    )
                }
            }
            A2uiNode.ChoicePicker.DisplayStyle.CHECKBOX -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Column(Modifier.selectableGroup()) {
                    node.options.forEachIndexed { index, option ->
                        if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                        val isSelected = option.value == selected
                        ListItem(
                            headlineContent = { Text(option.label) },
                            trailingContent = { RadioButton(selected = isSelected, onClick = null) },
                            modifier = Modifier.selectable(isSelected, role = Role.RadioButton) { onSelect(option.value) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) colors.primaryContainer else colors.surfaceContainerLow,
                                headlineColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                            ),
                        )
                    }
                }
            }
        }
        form.errors[node.path]?.let {
            Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun A2uiTextField(node: A2uiNode.TextField, form: FormState.Ready, onValueChange: (String) -> Unit) {
    val value = (form.values[node.path] as InputValue.Text).value
    val limit = form.lengthLimit?.takeIf { it.path == node.path }
    val length = if (limit == null) 0 else value.trim().length
    val overLimit = limit != null && length > limit.maximum
    val showCount = limit != null && length >= limit.showFrom
    val error = if (overLimit) limit.error else form.errors[node.path]
    val longText = node.variant == A2uiNode.TextField.Variant.LONG_TEXT
    val numeric = node.variant == A2uiNode.TextField.Variant.NUMBER
    val example = node.placeholder
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    val bringIntoView = remember { BringIntoViewRequester() }
    val request = form.focusRequest?.takeIf { it.path == node.path }

    LaunchedEffect(request) {
        if (request != null) {
            focus.requestFocus()
            bringIntoView.bringIntoView()
        }
    }

    Column(Modifier.bringIntoViewRequester(bringIntoView), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // An external label leaves the example visible even before the field has focus.
        if (example != null) {
            Text(
                node.label,
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.titleSmall,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics {
                if (example != null) contentDescription = node.label
            },
            label = if (example == null) { { Text(node.label) } } else null,
            placeholder = example?.let { { Text(it) } },
            textStyle = if (numeric) {
                MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace)
            } else MaterialTheme.typography.bodyLarge,
            isError = error != null,
            supportingText = if (error != null || showCount) {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(error.orEmpty(), Modifier.weight(1f), color = colors.error)
                            if (showCount) {
                                Text(
                                    stringResource(R.string.field_character_count, length, limit.maximum),
                                    color = if (overLimit) colors.error else colors.onSurfaceVariant,
                                )
                            }
                        }
                        if (showCount) {
                            LinearProgressIndicator(
                                progress = { (length.toFloat() / limit.maximum).coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                                color = if (overLimit) colors.error else colors.primary,
                                trackColor = colors.primaryContainer,
                            )
                        }
                    }
                }
            } else null,
            singleLine = !longText,
            minLines = if (longText) 3 else 1,
            maxLines = if (longText) 5 else 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Text
                },
            ),
        )
    }
}
