package app.funput.funput.ui.settings.speech

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.services.GoogleSpeechServices
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.settings.speech.model.SpeechSetupState

@Composable
internal fun SpeechSetupScreen(
    state: SpeechSetupState,
    onEnabled: (Boolean) -> Unit,
    onPermission: () -> Unit,
    onRefresh: (SpeechLocale) -> Unit,
    onDownload: (SpeechLocale) -> Unit,
    onBack: () -> Unit,
    preferredLocale: SpeechLocale? = null,
    guide: SpeechGuideActions = SpeechGuideActions(),
) {
    FunputScreen(stringResource(R.string.speech_settings_title), onBack = onBack) {
        item("voice-input") {
            FunputSection(title = null, footer = stringResource(R.string.speech_on_device_footer)) {
                ToggleRow(stringResource(R.string.speech_enabled), state.enabled, onEnabled,
                    enabled = state.featureAvailable && !state.saving,
                    modifier = Modifier.testTag("speech-enabled"))
                if (state.saveError) FunputRow(title = stringResource(R.string.speech_save_error))
            }
        }
        item("permission") {
            FunputSection(title = null, footer = stringResource(R.string.speech_return_to_keyboard)) {
                if (!state.featureAvailable) FunputRow(title = stringResource(R.string.speech_feature_unavailable))
                else if (state.availability == SpeechAvailability.UNSUPPORTED_OS) {
                    FunputRow(title = stringResource(R.string.speech_unsupported_os))
                }
                else if (state.permissionGranted) FunputRow(title = stringResource(R.string.speech_permission_granted))
                else ActionRow(stringResource(R.string.speech_permission_request), onPermission,
                    summary = stringResource(R.string.speech_permission_help),
                    modifier = Modifier.testTag("speech-permission"))
            }
        }
        if (state.featureAvailable && state.availability == SpeechAvailability.AVAILABLE) {
            item("languages") { SpeechLanguagesSection(state, preferredLocale, onRefresh, onDownload, guide.show) }
        } else if (state.featureAvailable && state.availability != SpeechAvailability.UNSUPPORTED_OS) {
            item("availability") { FunputSection(title = null) {
                FunputRow(title = stringResource(R.string.speech_service_unavailable))
            } }
        }
    }
    val context = guide.context
    if (state.guideVisible && context != null) {
        SpeechServicesSheet(context, guide.openPlay, guide.openSettings, guide.hide)
    }
}

/** The guide sheet's inputs; without a [context] (previews, tests) the sheet never shows. */
internal data class SpeechGuideActions(
    val context: SpeechGuideContext? = null,
    val show: () -> Unit = {},
    val hide: () -> Unit = {},
    val openPlay: () -> Unit = {},
    val openSettings: () -> Unit = {},
)

@Composable
private fun SpeechLanguagesSection(
    state: SpeechSetupState,
    preferredLocale: SpeechLocale?,
    onRefresh: (SpeechLocale) -> Unit,
    onDownload: (SpeechLocale) -> Unit,
    onShowGuide: () -> Unit,
) {
    val locales = SpeechLocale.entries.sortedBy { if (it == preferredLocale) 0 else 1 }
    val statuses = locales.associateWith { speechLocaleStatus(state.locales.getValue(it)) }
    val servicesMissing = state.speechServices != GoogleSpeechServices.Status.INSTALLED
    FunputSection(title = stringResource(R.string.speech_languages_title),
        footer = stringResource(R.string.speech_languages_footer)) {
        if (servicesMissing) {
            LinkRow(
                title = stringResource(if (state.speechServices == GoogleSpeechServices.Status.DISABLED)
                    R.string.speech_services_disabled else R.string.speech_services_missing),
                onClick = onShowGuide,
                summary = stringResource(R.string.speech_services_missing_summary),
                modifier = Modifier.testTag("speech-services-missing"),
            )
            FunputDivider()
        }
        locales.forEachIndexed { index, locale ->
            if (index > 0) FunputDivider()
            SpeechLocaleRow(locale, statuses.getValue(locale), { onDownload(locale) }, { onRefresh(locale) })
        }
        if (!servicesMissing && statuses.values.any { it.needsGuide }) {
            FunputDivider()
            LinkRow(stringResource(R.string.speech_open_guide), onShowGuide, modifier = Modifier.testTag("speech-open-guide"))
        }
    }
}
