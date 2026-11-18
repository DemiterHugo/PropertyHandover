package io.github.demiterhugo.propertyhandover

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.IntentCompat
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandoverFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun damageShowsFieldErrorsAndKeepsItsDraftWhenSwitching() {
        choose("Daño en la vivienda")
        awaitField("Estancia")
        click("Ver resumen")
        compose.onAllNodes(hasText("Este campo es obligatorio.")).assertCountEquals(2)

        enter("Estancia", "Salon")
        enter("Descripción", "Marca en la pared")
        choose("Lectura de contador")
        awaitField("Lectura")
        compose.onNodeWithText("Tipo de suministro").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Estancia").assertDoesNotExist()
        choose("Daño en la vivienda")
        click("Ver resumen")

        compose.onNodeWithText("Daño en la vivienda\nEstancia: Salon\nDescripción: Marca en la pared")
            .performScrollTo().assertIsDisplayed()
        enter("Estancia", "Cocina")
        compose.onNodeWithText("Compartir resumen").assertDoesNotExist()
    }

    @Test
    fun appStartsUnselectedAndSharesTheExactSummaryAfterChoosingMeter() {
        val damage = option("Daño en la vivienda")
        val meter = option("Lectura de contador")
        damage.assertIsDisplayed().assertIsNotSelected()
        meter.assertIsDisplayed().assertIsNotSelected()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)

        choose("Lectura de contador")
        meter.assertIsSelected()
        damage.assertIsNotSelected()
        awaitField("Lectura")
        compose.onNode(hasSetTextAction() and hasText("Lectura")).assertIsNotFocused()
        compose.onNodeWithText("Descripción").assertDoesNotExist()
        click("Ver resumen")
        compose.onNodeWithText("Elige un tipo de suministro.").performScrollTo().assertIsDisplayed()
        click("Agua")
        enter("Lectura", "-1")
        click("Ver resumen")
        compose.onNodeWithText("Introduce una lectura igual o mayor que cero.").performScrollTo().assertIsDisplayed()
        enter("Lectura", "128,4")
        click("Ver resumen")

        val summary = "Lectura de contador\nSuministro: Agua\nLectura: 128,4"
        compose.onNodeWithText(summary).performScrollTo().assertIsDisplayed()
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CHOOSER))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            click("Compartir resumen")
            val chooser = Intents.getIntents().single { it.action == Intent.ACTION_CHOOSER }
            val send = checkNotNull(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java))
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            assertEquals(summary, send.getStringExtra(Intent.EXTRA_TEXT))
        } finally {
            Intents.release()
        }
    }

    private fun awaitField(label: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction() and hasText(label)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun option(title: String) = compose.onNode(
        hasText(title) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
    )

    private fun choose(title: String) {
        option(title).performScrollTo().performClick()
    }

    private fun click(label: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
    }

    private fun enter(label: String, value: String) {
        compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value)
    }
}
