package io.github.demiterhugo.propertyhandover

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
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
        compose.onNodeWithText("Detalles del daño").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ej. Cocina").assertExists()
        compose.onNodeWithText("Ej. Baldosa rota junto a la puerta").assertExists()
        click("Guardar en el acta")
        compose.onNodeWithText("Indica la estancia.").assertExists()
        compose.onNodeWithText("Describe el daño.").assertExists()
        compose.onNode(field("Estancia")).assertIsFocused()

        enter("Estancia", "Salon")
        enter("Descripción del daño", "Marca en la pared")
        choose("Lectura de contador")
        awaitField("Lectura")
        compose.onNodeWithText("Tipo de suministro").performScrollTo().assertIsDisplayed()
        compose.onNode(field("Estancia")).assertDoesNotExist()
        choose("Daño en la vivienda")
        click("Guardar en el acta")

        openReport(1)
        compose.onNodeWithText("Daño en la vivienda\nEstancia: Salon\nDescripción: Marca en la pared")
            .performScrollTo().assertIsDisplayed()
        click("Cerrar")
        enter("Estancia", "Cocina")
        compose.onNodeWithText("Vuelve a guardar para actualizar el acta.").assertExists()
        click("Guardar en el acta")
        openReport(1)
        compose.onNodeWithText("Daño en la vivienda\nEstancia: Cocina\nDescripción: Marca en la pared")
            .performScrollTo().assertIsDisplayed()
        compose.onNode(hasContentDescription("Quitar del acta: Daño en la vivienda"))
            .performScrollTo().performClick()
        compose.onNodeWithText("Guarda un daño o una lectura para empezar el acta.").assertExists()
        compose.onNodeWithText("Compartir acta").assertDoesNotExist()
    }

    @Test
    fun damageShowsTheCounterAndFocusesTheOverLimitDescription() {
        choose("Daño en la vivienda")
        awaitField("Estancia")
        enter("Estancia", "Cocina")
        enter("Descripción del daño", "a".repeat(199))
        compose.onNodeWithText("199 / 240").assertDoesNotExist()
        enter("Descripción del daño", "a".repeat(200))
        compose.onNodeWithText("200 / 240").performScrollTo().assertIsDisplayed()
        enter("Descripción del daño", "a".repeat(241))
        compose.onNodeWithText("Reduce la descripción a 240 caracteres.").assertExists()
        click("Guardar en el acta")
        compose.onNode(field("Descripción del daño")).assertIsFocused()
        compose.onNodeWithText("Guardado en el acta").assertDoesNotExist()
        enter("Descripción del daño", "Baldosa rota junto a la puerta")
        compose.onNodeWithText("Reduce la descripción a 240 caracteres.").assertDoesNotExist()
        click("Guardar en el acta")
        compose.onNodeWithText("Guardado en el acta").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun expandedInspectorFollowsTheSelectedFormAndShowsItsSource() {
        compose.onNodeWithText("Cada opción carga un formulario A2UI que se muestra con Jetpack Compose.")
            .assertIsDisplayed()
        choose("Daño en la vivienda")
        awaitField("Estancia")
        compose.onNodeWithText("Estancia → TextField").assertDoesNotExist()
        click("Cómo se genera")
        compose.onNodeWithText("Estancia → TextField").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Descripción del daño → TextField").performScrollTo().assertIsDisplayed()
        click("Ver JSON completo")
        compose.onNodeWithText("\"surfaceId\": \"damage\"", substring = true).assertExists()
        compose.onNodeWithText("\"@path\": \"/room\"", substring = true).assertExists()
        compose.onNodeWithText("Cerrar").performClick()

        choose("Lectura de contador")
        awaitField("Lectura")
        compose.onNodeWithText("Estancia → TextField").assertDoesNotExist()
        compose.onNodeWithText("Tipo de suministro → ChoicePicker").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Lectura → TextField").performScrollTo().assertIsDisplayed()
        click("Ver JSON completo")
        compose.onNodeWithText("\"surfaceId\": \"meter\"", substring = true).assertExists()
        compose.onNodeWithText("\"component\": \"ChoicePicker\"", substring = true).assertExists()
        compose.onNodeWithText("\"@path\": \"/room\"", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Cerrar").performClick()
        click("Cómo se genera")
        compose.onNodeWithText("Tipo de suministro → ChoicePicker").assertDoesNotExist()
    }

    @Test
    fun appStartsUnselectedAndSharesBothValidatedEntriesInOneReport() {
        val damage = option("Daño en la vivienda")
        val meter = option("Lectura de contador")
        damage.assertIsDisplayed().assertIsNotSelected()
        meter.assertIsDisplayed().assertIsNotSelected()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)

        choose("Lectura de contador")
        meter.assertIsSelected()
        damage.assertIsNotSelected()
        awaitField("Lectura")
        compose.onNode(field("Lectura")).assertIsNotFocused()
        compose.onNode(field("Descripción del daño")).assertDoesNotExist()
        click("Guardar en el acta")
        compose.onNodeWithText("Elige un tipo de suministro.").performScrollTo().assertIsDisplayed()
        option("Agua").performScrollTo().assertIsNotSelected().performClick()
        option("Gas").performScrollTo().performClick().assertIsSelected()
        option("Agua").assertIsNotSelected().performClick().assertIsSelected()
        option("Gas").assertIsNotSelected()
        enter("Lectura", "-1")
        click("Guardar en el acta")
        compose.onNodeWithText("Introduce una lectura igual o mayor que cero.").performScrollTo().assertIsDisplayed()
        enter("Lectura", "128,4")
        click("Guardar en el acta")

        choose("Daño en la vivienda")
        awaitField("Estancia")
        enter("Estancia", "Cocina")
        enter("Descripción del daño", "Baldosa rota")
        click("Guardar en el acta")

        val damageSummary = "Daño en la vivienda\nEstancia: Cocina\nDescripción: Baldosa rota"
        val meterSummary = "Lectura de contador\nSuministro: Agua\nLectura: 128,4"
        openReport(2)
        compose.onNodeWithText(damageSummary).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(meterSummary).performScrollTo().assertIsDisplayed()
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CHOOSER))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            click("Compartir acta")
            val chooser = Intents.getIntents().single { it.action == Intent.ACTION_CHOOSER }
            val send = checkNotNull(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java))
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            assertEquals("Acta de entrega\n\n$damageSummary\n\n$meterSummary", send.getStringExtra(Intent.EXTRA_TEXT))
        } finally {
            Intents.release()
        }
    }

    private fun openReport(entryCount: Int) {
        val label = "Ver acta · $entryCount/2"
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(label)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(label).assertIsDisplayed().performClick()
    }

    private fun awaitField(label: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(field(label)).fetchSemanticsNodes().isNotEmpty()
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
        compose.onNode(field(label)).performScrollTo().performTextReplacement(value)
    }

    private fun field(label: String) = hasSetTextAction() and (hasContentDescription(label) or hasText(label))
}
