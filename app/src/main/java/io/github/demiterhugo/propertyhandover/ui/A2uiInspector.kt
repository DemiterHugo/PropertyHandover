package io.github.demiterhugo.propertyhandover.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.data.A2uiNode
import io.github.demiterhugo.propertyhandover.data.A2uiSurface

@Composable
internal fun A2uiInspector(surface: A2uiSurface, expanded: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val expansionState = stringResource(
        if (expanded) R.string.a2ui_inspector_expanded else R.string.a2ui_inspector_collapsed,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column {
            TextButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = expansionState },
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.a2ui_inspector_title), Modifier.weight(1f))
                    Text(if (expanded) "−" else "+", Modifier.clearAndSetSemantics {})
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    A2uiInputRows(surface.root)
                    A2uiMessageAction(surface)
                }
            }
        }
    }
}

@Composable
private fun A2uiInputRows(node: A2uiNode) {
    when (node) {
        is A2uiNode.Card -> A2uiInputRows(node.child)
        is A2uiNode.Column -> node.children.forEach { A2uiInputRows(it) }
        is A2uiNode.TextField -> A2uiInputRow(node.label, "TextField")
        is A2uiNode.ChoicePicker -> A2uiInputRow(node.label, "ChoicePicker")
        is A2uiNode.Button, is A2uiNode.Text -> Unit
    }
}

@Composable
private fun A2uiInputRow(label: String, component: String) {
    Text(
        text = stringResource(R.string.a2ui_field_component, label, component),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun A2uiMessageAction(surface: A2uiSurface) {
    var visible by rememberSaveable(surface.id) { mutableStateOf(false) }
    TextButton(onClick = { visible = true }) { Text(stringResource(R.string.view_a2ui_message)) }
    if (visible) {
        AlertDialog(
            onDismissRequest = { visible = false },
            title = { Text(stringResource(R.string.a2ui_message_title)) },
            text = {
                SelectionContainer(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = surface.sourceMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { visible = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}
