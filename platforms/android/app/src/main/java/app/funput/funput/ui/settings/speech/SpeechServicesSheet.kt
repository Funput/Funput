package app.funput.funput.ui.settings.speech

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.speech.services.GoogleSpeechServices
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputUi

/** What the guide can offer on this device. */
internal data class SpeechGuideContext(
    val services: GoogleSpeechServices.Status,
    val hasPlayStore: Boolean,
    /** ROMs that block Speech Services' consent dialog unless its pop-up permission is on. */
    val popupPermissionRom: Boolean,
)

private val PopupPermissionRoms = setOf("xiaomi", "redmi", "poco", "oppo", "oneplus", "realme", "vivo", "iqoo")

internal fun isPopupPermissionRom(manufacturer: String): Boolean = manufacturer.lowercase() in PopupPermissionRoms

/** How to get the Google app that downloads and runs the speech model, as numbered steps. */
@Composable
internal fun SpeechServicesSheet(
    context: SpeechGuideContext,
    onOpenPlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val missing = context.services == GoogleSpeechServices.Status.MISSING
    val canInstall = context.hasPlayStore && context.services != GoogleSpeechServices.Status.DISABLED
    FunputSheet(onDismiss = onDismiss, title = stringResource(R.string.speech_guide_title)) {
        BasicText(
            text = stringResource(if (missing && !context.hasPlayStore) R.string.speech_guide_no_play
                else R.string.speech_guide_intro),
            style = FunputUi.typography.body.copy(color = FunputUi.colors.secondaryLabel),
        )
        if (!missing || context.hasPlayStore) {
            FunputSection(title = null, modifier = Modifier.testTag("speech-guide-steps")) {
                guideSteps(context).forEachIndexed { index, step ->
                    if (index > 0) FunputDivider()
                    FunputRow(
                        title = "${index + 1}. ${stringResource(step.title)}",
                        summary = stringResource(step.summary),
                        modifier = Modifier.testTag("speech-guide-${step.name.lowercase()}"),
                    )
                }
            }
        }
        if (canInstall) {
            FunputButton(stringResource(R.string.speech_guide_open_play), onOpenPlay,
                modifier = Modifier.fillMaxWidth().testTag("speech-guide-play"))
        }
        if (!missing) {
            FunputButton(stringResource(R.string.speech_guide_open_settings), onOpenSettings,
                style = if (canInstall) FunputButtonStyle.SECONDARY else FunputButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth().testTag("speech-guide-settings"))
        }
        FunputButton(stringResource(R.string.speech_guide_later), onDismiss, style = FunputButtonStyle.PLAIN,
            modifier = Modifier.fillMaxWidth())
    }
}

internal enum class SpeechGuideStep(val title: Int, val summary: Int) {
    INSTALL(R.string.speech_guide_install, R.string.speech_guide_install_summary),
    ENABLE(R.string.speech_guide_enable, R.string.speech_guide_enable_summary),
    POPUP(R.string.speech_guide_popup, R.string.speech_guide_popup_summary),
    RETURN(R.string.speech_guide_return, R.string.speech_guide_return_summary),
}

internal fun guideSteps(context: SpeechGuideContext): List<SpeechGuideStep> = buildList {
    add(if (context.services == GoogleSpeechServices.Status.DISABLED) SpeechGuideStep.ENABLE else SpeechGuideStep.INSTALL)
    if (context.popupPermissionRom) add(SpeechGuideStep.POPUP)
    add(SpeechGuideStep.RETURN)
}
