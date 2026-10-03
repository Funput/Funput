package app.funput.funput.speech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupRequest
import app.funput.funput.ui.rememberFunputSettings
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.speech.SpeechSetupRoute
import app.funput.funput.ui.theme.resolveDarkTheme

/** Non-exported preparation host. Permission grant never starts a recording. */
class SpeechSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!resources.getBoolean(app.funput.funput.ime.R.bool.speech_feature_available)) {
            finish()
            return
        }
        val request = SpeechSetupRequest.read(intent)
        enableEdgeToEdge()
        setContent {
            val settings = rememberFunputSettings()
            FunputUiTheme(settings.appearanceMode.resolveDarkTheme(isSystemInDarkTheme())) {
                SpeechSetupRoute(onBack = ::finish, preferredLocale = request.locale)
            }
        }
    }
}
