package io.github.demiterhugo.propertyhandover.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.demiterhugo.propertyhandover.R
import io.github.demiterhugo.propertyhandover.data.A2uiException
import io.github.demiterhugo.propertyhandover.data.A2uiSurface
import io.github.demiterhugo.propertyhandover.data.InputValue
import io.github.demiterhugo.propertyhandover.data.LocalA2uiForms
import io.github.demiterhugo.propertyhandover.domain.EntryType
import io.github.demiterhugo.propertyhandover.domain.Handover
import io.github.demiterhugo.propertyhandover.domain.HandoverField
import io.github.demiterhugo.propertyhandover.domain.HandoverResult
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class HandoverUiState(
    val selectedType: EntryType = EntryType.DAMAGE,
    val forms: Map<EntryType, FormState> = emptyMap(),
)

internal sealed interface FormState {
    data class Ready(
        val surface: A2uiSurface,
        val values: Map<String, InputValue> = surface.initialValues,
        val errors: Map<String, String> = emptyMap(),
        val summary: String? = null,
    ) : FormState

    data class Failed(val message: Int) : FormState
}

@HiltViewModel
internal class HandoverViewModel @Inject constructor(
    private val localForms: LocalA2uiForms,
    private val handover: Handover,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HandoverUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val forms = withContext(Dispatchers.IO) { EntryType.entries.associateWith(::loadForm) }
            mutableState.update { it.copy(forms = forms) }
        }
    }

    fun select(type: EntryType) {
        mutableState.update { state ->
            if (state.selectedType == type) return@update state
            val previous = state.forms[state.selectedType] as? FormState.Ready
            val forms = if (previous == null) state.forms else {
                state.forms + (state.selectedType to previous.copy(summary = null))
            }
            state.copy(selectedType = type, forms = forms)
        }
    }

    fun updateValue(type: EntryType, path: String, value: InputValue) = updateForm(type) { form ->
        form.copy(values = form.values + (path to value), errors = form.errors - path, summary = null)
    }

    fun handleEvent(type: EntryType, name: String) {
        if (name != "showSummary") return
        updateForm(type) { form ->
            val fields = formPaths.getValue(type)
            val inputs = fields.mapValues { (_, path) ->
                when (val value = form.values.getValue(path)) {
                    is InputValue.Text -> value.value
                    is InputValue.Choices -> value.values.singleOrNull().orEmpty()
                }
            }
            when (val result = handover.summarize(type, inputs)) {
                is HandoverResult.Summary -> form.copy(errors = emptyMap(), summary = result.text)
                is HandoverResult.Invalid -> form.copy(
                    errors = result.errors.entries.associate { (field, error) -> fields.getValue(field) to error.message },
                    summary = null,
                )
            }
        }
    }

    private fun loadForm(type: EntryType): FormState = try {
        val surface = localForms.load(type)
        if (!surface.initialValues.keys.containsAll(formPaths.getValue(type).values)) {
            FormState.Failed(R.string.form_invalid)
        } else {
            FormState.Ready(surface)
        }
    } catch (_: A2uiException.Invalid) {
        FormState.Failed(R.string.form_invalid)
    } catch (_: A2uiException.Unsupported) {
        FormState.Failed(R.string.form_unsupported)
    } catch (_: IOException) {
        FormState.Failed(R.string.form_unavailable)
    }

    private fun updateForm(type: EntryType, transform: (FormState.Ready) -> FormState.Ready) {
        mutableState.update { state ->
            val form = state.forms[type] as? FormState.Ready
            if (type != state.selectedType || form == null) return@update state
            state.copy(forms = state.forms + (type to transform(form)))
        }
    }

    private companion object {
        val formPaths = mapOf(
            EntryType.DAMAGE to mapOf(HandoverField.ROOM to "/room", HandoverField.DESCRIPTION to "/description"),
            EntryType.METER to mapOf(HandoverField.UTILITY to "/utility", HandoverField.READING to "/reading"),
        )
    }
}
