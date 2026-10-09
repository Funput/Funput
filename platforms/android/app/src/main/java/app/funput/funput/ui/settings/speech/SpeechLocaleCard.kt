package app.funput.funput.ui.settings.speech

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.settings.speech.model.SpeechLocaleState

@Composable
internal fun SpeechLocaleCard(locale: SpeechLocale, state: SpeechLocaleState, refresh: () -> Unit, download: () -> Unit) {
    FunputSection(title = stringResource(if (locale == SpeechLocale.VI) R.string.speech_vietnamese
        else R.string.speech_english), footer = stringResource(R.string.speech_download_footer)) {
        FunputRow(title = speechLocaleMessage(state), modifier = Modifier.testTag("speech-model-${locale.tag}")
            .semantics { liveRegion = LiveRegionMode.Polite })
        if (!state.checking && !state.downloading) {
            FunputDivider()
            ActionRow(stringResource(R.string.speech_check_again), refresh,
                modifier = Modifier.testTag("speech-check-${locale.tag}"))
            if (state.capability == SpeechCapability.DOWNLOADABLE) {
                FunputDivider()
                ActionRow(stringResource(R.string.speech_download), download,
                    modifier = Modifier.testTag("speech-download-${locale.tag}"))
            }
        }
    }
}

@Composable
private fun speechLocaleMessage(state: SpeechLocaleState): String = when {
    state.checking -> stringResource(R.string.speech_checking)
    state.download is SpeechDownloadEvent.Progress -> stringResource(R.string.speech_download_progress, state.download.percent)
    state.downloading -> stringResource(R.string.speech_downloading)
    state.download == SpeechDownloadEvent.Requested -> stringResource(R.string.speech_download_requested)
    state.download == SpeechDownloadEvent.Scheduled -> stringResource(R.string.speech_pending)
    state.download is SpeechDownloadEvent.Failure -> stringResource(R.string.speech_download_error)
    else -> stringResource(when (state.capability) {
        SpeechCapability.READY -> R.string.speech_ready
        SpeechCapability.PENDING -> R.string.speech_pending
        SpeechCapability.DOWNLOADABLE -> R.string.speech_downloadable
        SpeechCapability.UNSUPPORTED -> R.string.speech_unsupported_locale
        SpeechCapability.UNKNOWN -> R.string.speech_unknown_locale
    })
}
