package io.github.demiterhugo.propertyhandover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.domain.EntryType
import io.github.demiterhugo.propertyhandover.domain.HandoverReport
import io.github.demiterhugo.propertyhandover.ui.theme.forEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HandoverReportSheet(
    report: HandoverReport,
    onRemoveEntry: (EntryType) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.report_title)
    val shareText = report.shareText(title)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            if (shareText == null) {
                Text(stringResource(R.string.report_empty), style = MaterialTheme.typography.bodyMedium)
            }
            for (type in EntryType.entries) {
                val summary = report.entries[type] ?: continue
                ReportEntry(type, summary.text) { onRemoveEntry(type) }
            }
            if (shareText != null) {
                Button(onClick = { onShare(shareText) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.share_report))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun ReportEntry(type: EntryType, summary: String, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme.forEntry(type)
    val removeLabel = stringResource(R.string.remove_named_report_entry, summary.substringBefore('\n'))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
            Text(summary, style = MaterialTheme.typography.bodyLarge)
            TextButton(
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = removeLabel },
            ) {
                Text(stringResource(R.string.remove_report_entry))
            }
        }
    }
}
