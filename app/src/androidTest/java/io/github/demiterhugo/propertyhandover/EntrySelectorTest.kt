package io.github.demiterhugo.propertyhandover

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.demiterhugo.propertyhandover.ui.HandoverScreen
import io.github.demiterhugo.propertyhandover.ui.HandoverUiState
import io.github.demiterhugo.propertyhandover.ui.theme.PropertyHandoverTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntrySelectorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun optionTitlesAndHintsFitAtLargeFontScale() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                PropertyHandoverTheme {
                    HandoverScreen(
                        state = HandoverUiState(),
                        onSelectType = {},
                        onValueChange = { _, _, _ -> },
                        onEvent = { _, _ -> },
                        onRemoveEntry = {},
                        onShare = {},
                    )
                }
            }
        }

        assertTextFits("Daño en la vivienda")
        assertTextFits("Estancia y descripción")
        assertTextFits("Lectura de contador")
        assertTextFits("Agua, electricidad o gas")
    }

    private fun assertTextFits(label: String) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertFalse(label, results.single().hasVisualOverflow)
    }
}
