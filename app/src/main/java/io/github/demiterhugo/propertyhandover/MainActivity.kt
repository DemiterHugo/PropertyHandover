package io.github.demiterhugo.propertyhandover

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.demiterhugo.propertyhandover.ui.HandoverScreen
import io.github.demiterhugo.propertyhandover.ui.HandoverViewModel
import io.github.demiterhugo.propertyhandover.ui.theme.PropertyHandoverTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HandoverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PropertyHandoverTheme {
                HandoverScreen(
                    state = state,
                    onSelectType = viewModel::select,
                    onValueChange = viewModel::updateValue,
                    onEvent = viewModel::handleEvent,
                    onShare = ::shareSummary,
                )
            }
        }
    }

    private fun shareSummary(summary: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, summary)
        startActivity(Intent.createChooser(send, getString(R.string.share_summary)))
    }
}
