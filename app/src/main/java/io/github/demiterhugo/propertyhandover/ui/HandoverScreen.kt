package io.github.demiterhugo.propertyhandover.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.data.InputValue
import io.github.demiterhugo.propertyhandover.domain.EntryType

@Composable
internal fun HandoverScreen(
    state: HandoverUiState,
    onSelectType: (EntryType) -> Unit,
    onValueChange: (EntryType, String, InputValue) -> Unit,
    onEvent: (EntryType, String) -> Unit,
    onShare: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val selectedType = state.selectedType
    Scaffold(topBar = { HandoverTopBar() }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                EntrySelector(selectedType) { type ->
                    if (type != selectedType) focusManager.clearFocus()
                    onSelectType(type)
                }
                if (selectedType == null) return@Column

                when (val form = state.forms[selectedType]) {
                    null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    is FormState.Failed -> Text(
                        stringResource(form.message),
                        color = MaterialTheme.colorScheme.error,
                    )
                    is FormState.Ready -> key(form.surface.id) {
                        A2uiForm(
                            node = form.surface.root,
                            form = form,
                            onValueChange = { path, value -> onValueChange(selectedType, path, value) },
                            onEvent = { event ->
                                focusManager.clearFocus()
                                onEvent(selectedType, event)
                            },
                        )
                        form.summary?.let { SummaryCard(it) { onShare(it) } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandoverTopBar() {
    val colors = MaterialTheme.colorScheme
    Column {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = colors.surfaceVariant) {
                    Icon(
                        painter = painterResource(R.drawable.ic_handover_key),
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp),
                        tint = colors.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            }
        })
        HorizontalDivider(color = colors.outlineVariant)
    }
}

@Composable
private fun EntrySelector(selectedType: EntryType?, onSelect: (EntryType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.entry_prompt),
            modifier = Modifier.semantics { heading() },
            style = if (selectedType == null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
        )
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (type in EntryType.entries) {
                val (title, hint) = when (type) {
                    EntryType.DAMAGE -> R.string.entry_damage to R.string.entry_damage_hint
                    EntryType.METER -> R.string.entry_meter to R.string.entry_meter_hint
                }
                EntryOption(
                    title = stringResource(title),
                    hint = stringResource(hint),
                    selected = selectedType == type,
                    onSelect = { onSelect(type) },
                )
            }
        }
    }
}

@Composable
private fun EntryOption(title: String, hint: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .selectable(selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
                Text(
                    hint,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                )
            }
            Box(Modifier.widthIn(min = 24.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Text(
                        text = "✓",
                        modifier = Modifier.clearAndSetSemantics {},
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: String, onShare: () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(summary) { requester.bringIntoView() }

    Card(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.share_summary))
            }
        }
    }
}
