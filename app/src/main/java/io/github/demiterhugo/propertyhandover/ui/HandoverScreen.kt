package io.github.demiterhugo.propertyhandover.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.data.InputValue
import io.github.demiterhugo.propertyhandover.domain.EntryType
import io.github.demiterhugo.propertyhandover.ui.theme.BrandColor
import io.github.demiterhugo.propertyhandover.ui.theme.forEntry

@Composable
internal fun HandoverScreen(
    state: HandoverUiState,
    onSelectType: (EntryType) -> Unit,
    onValueChange: (EntryType, String, InputValue) -> Unit,
    onEvent: (EntryType, String) -> Unit,
    onRemoveEntry: (EntryType) -> Unit,
    onShare: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val selectedType = state.selectedType
    var inspectorExpanded by rememberSaveable { mutableStateOf(false) }
    var reportVisible by rememberSaveable { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        topBar = { HandoverTopBar() },
        bottomBar = {
            if (!keyboardVisible) {
                ReportBar(state.report.entries.size) {
                    focusManager.clearFocus()
                    reportVisible = true
                }
            }
        },
    ) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
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
                    is FormState.Ready -> {
                        A2uiInspector(
                            surface = form.surface,
                            expanded = inspectorExpanded,
                            onToggle = { inspectorExpanded = !inspectorExpanded },
                        )
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.forEntry(selectedType)) {
                            key(form.surface.id) {
                                A2uiForm(
                                    node = form.surface.root,
                                    form = form,
                                    onValueChange = { path, value -> onValueChange(selectedType, path, value) },
                                    onEvent = { event ->
                                        focusManager.clearFocus()
                                        onEvent(selectedType, event)
                                    },
                                )
                                if (selectedType in state.report.entries) {
                                    Text(
                                        text = stringResource(if (form.saved) R.string.entry_saved else R.string.entry_unsaved),
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (reportVisible) {
        HandoverReportSheet(
            report = state.report,
            onRemoveEntry = onRemoveEntry,
            onShare = onShare,
            onDismiss = { reportVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandoverTopBar() {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.app_name), textAlign = TextAlign.Center) },
        navigationIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_handover_key),
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandColor,
            scrolledContainerColor = BrandColor,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
        ),
    )
}

@Composable
private fun EntrySelector(selectedType: EntryType?, onSelect: (EntryType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.entry_prompt),
                modifier = Modifier.semantics { heading() },
                style = if (selectedType == null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.entry_a2ui_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (type in EntryType.entries) {
                val (title, hint, icon) = when (type) {
                    EntryType.DAMAGE -> Triple(R.string.entry_damage, R.string.entry_damage_hint, R.drawable.ic_handover_home)
                    EntryType.METER -> Triple(R.string.entry_meter, R.string.entry_meter_hint, R.drawable.ic_handover_meter)
                }
                EntryOption(
                    title = stringResource(title),
                    hint = stringResource(hint),
                    icon = icon,
                    type = type,
                    selected = selectedType == type,
                    onSelect = { onSelect(type) },
                )
            }
        }
    }
}

@Composable
private fun EntryOption(
    title: String,
    hint: String,
    @DrawableRes icon: Int,
    type: EntryType,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val entryColors = colors.forEntry(type)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) entryColors.primaryContainer else colors.surfaceContainerLow,
        contentColor = if (selected) entryColors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, if (selected) entryColors.primary else colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .selectable(selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) entryColors.primary else entryColors.primaryContainer,
                contentColor = if (selected) entryColors.onPrimary else entryColors.onPrimaryContainer,
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(8.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
                Text(
                    hint,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) entryColors.onPrimaryContainer else colors.onSurfaceVariant,
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
private fun ReportBar(entryCount: Int, onOpen: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.open_report, entryCount, EntryType.entries.size))
            }
        }
    }
}
