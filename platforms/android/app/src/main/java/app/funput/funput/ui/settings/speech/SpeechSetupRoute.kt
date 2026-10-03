package app.funput.funput.ui.settings.speech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.funput.funput.ime.settings.speech.VoiceInputSettings
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.OnDeviceSpeechPreparationService
import app.funput.funput.ui.settings.speech.model.SpeechSetupModel
import app.funput.funput.ui.settings.speech.permission.hasSpeechPermission
import app.funput.funput.ui.settings.speech.permission.rememberSpeechPermissionRequest

/** Shared by Settings navigation and the IME's non-recording setup Activity. */
@Composable
internal fun SpeechSetupRoute(onBack: () -> Unit, preferredLocale: SpeechLocale? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember(context, scope) {
        val store = VoiceInputSettings(context)
        SpeechSetupModel(OnDeviceSpeechPreparationService(context), store.voiceInputEnabled,
            store::setVoiceInputEnabled, scope,
            context.resources.getBoolean(app.funput.funput.ime.R.bool.speech_feature_available))
    }
    val permissionRequest = rememberSpeechPermissionRequest(model::permissionResult)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(model, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> model.resume(context.hasSpeechPermission())
                Lifecycle.Event.ON_PAUSE -> model.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) model.resume(context.hasSpeechPermission())
        onDispose {
            lifecycle.removeObserver(observer)
            model.close()
        }
    }
    SpeechSetupScreen(model.state, model::setEnabled, permissionRequest,
        model::refresh, model::download, onBack, preferredLocale)
}
