package app.funput.funput.ui.settings.speech

import android.speech.SpeechRecognizer
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.speech.model.SpeechLocaleState

/** The one thing a language row offers at its end. */
internal enum class SpeechLocaleAction { READY, BUSY, DOWNLOAD, RETRY_DOWNLOAD, RETRY_CHECK }

/** What a language row says, and the single action it shows for that state. */
internal data class SpeechLocaleStatus(
    @StringRes val message: Int,
    val action: SpeechLocaleAction,
    val progress: Int? = null,
) {
    /** A failure the guide sheet may explain: missing app, blocked dialog, unknown service. */
    val needsGuide: Boolean
        get() = action == SpeechLocaleAction.RETRY_CHECK ||
            (action == SpeechLocaleAction.RETRY_DOWNLOAD && message != R.string.speech_network_error)
}

internal fun speechLocaleStatus(state: SpeechLocaleState): SpeechLocaleStatus {
    val download = state.download
    return when {
        state.checking -> SpeechLocaleStatus(R.string.speech_checking, SpeechLocaleAction.BUSY)
        state.installing -> SpeechLocaleStatus(R.string.speech_installing, SpeechLocaleAction.BUSY)
        download is SpeechDownloadEvent.Progress ->
            SpeechLocaleStatus(R.string.speech_download_progress, SpeechLocaleAction.BUSY, download.percent)
        state.downloading -> SpeechLocaleStatus(R.string.speech_preparing, SpeechLocaleAction.BUSY)
        download == SpeechDownloadEvent.Requested ->
            SpeechLocaleStatus(R.string.speech_download_requested, SpeechLocaleAction.BUSY)
        download == SpeechDownloadEvent.Scheduled -> SpeechLocaleStatus(R.string.speech_pending, SpeechLocaleAction.BUSY)
        download is SpeechDownloadEvent.Failure -> when (download.code) {
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                SpeechLocaleStatus(R.string.speech_network_error, SpeechLocaleAction.RETRY_DOWNLOAD)
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                SpeechLocaleStatus(R.string.speech_unsupported_locale, SpeechLocaleAction.RETRY_CHECK)
            else -> SpeechLocaleStatus(R.string.speech_download_error, SpeechLocaleAction.RETRY_DOWNLOAD)
        }
        else -> when (state.capability) {
            SpeechCapability.READY -> SpeechLocaleStatus(R.string.speech_ready, SpeechLocaleAction.READY)
            SpeechCapability.PENDING -> SpeechLocaleStatus(R.string.speech_pending, SpeechLocaleAction.BUSY)
            SpeechCapability.DOWNLOADABLE -> SpeechLocaleStatus(R.string.speech_downloadable, SpeechLocaleAction.DOWNLOAD)
            // Also what Speech Services reports before it has fetched its language list (fresh
            // install, cleared data), so it is never final: offer a re-check and the guide.
            SpeechCapability.UNSUPPORTED ->
                SpeechLocaleStatus(R.string.speech_unsupported_locale, SpeechLocaleAction.RETRY_CHECK)
            SpeechCapability.UNKNOWN -> SpeechLocaleStatus(R.string.speech_unknown_locale, SpeechLocaleAction.RETRY_CHECK)
        }
    }
}

@StringRes
internal fun SpeechLocale.titleRes(): Int = if (this == SpeechLocale.VI) R.string.speech_vietnamese else R.string.speech_english

@Composable
internal fun SpeechLocaleRow(locale: SpeechLocale, status: SpeechLocaleStatus, download: () -> Unit, refresh: () -> Unit) {
    val message = status.progress?.let { stringResource(status.message, it) } ?: stringResource(status.message)
    FunputRow(
        title = stringResource(locale.titleRes()),
        summary = message,
        modifier = Modifier.testTag("speech-model-${locale.tag}").semantics { liveRegion = LiveRegionMode.Polite },
        accessory = { SpeechLocaleAccessory(locale, status.action, download, refresh) },
    )
}

@Composable
private fun SpeechLocaleAccessory(locale: SpeechLocale, action: SpeechLocaleAction, download: () -> Unit, refresh: () -> Unit) {
    when (action) {
        SpeechLocaleAction.BUSY -> {
            val description = stringResource(R.string.speech_busy_description)
            SpeechBusyIndicator(Modifier.semantics { contentDescription = description })
        }
        SpeechLocaleAction.READY -> {
            val description = stringResource(R.string.speech_ready_description)
            Image(
                painter = painterResource(FunputIcons.Check),
                contentDescription = description,
                colorFilter = ColorFilter.tint(FunputUi.colors.success),
                modifier = Modifier.size(20.dp),
            )
        }
        SpeechLocaleAction.DOWNLOAD -> FunputButton(stringResource(R.string.speech_download), download,
            style = FunputButtonStyle.SECONDARY, modifier = Modifier.testTag("speech-download-${locale.tag}"))
        SpeechLocaleAction.RETRY_DOWNLOAD -> FunputButton(stringResource(R.string.speech_retry), download,
            style = FunputButtonStyle.SECONDARY, modifier = Modifier.testTag("speech-retry-${locale.tag}"))
        SpeechLocaleAction.RETRY_CHECK -> FunputButton(stringResource(R.string.speech_retry), refresh,
            style = FunputButtonStyle.SECONDARY, modifier = Modifier.testTag("speech-retry-${locale.tag}"))
    }
}
