package app.funput.funput.catalog

import android.os.Bundle
import app.funput.funput.speech.catalog.SpeechPanelCatalogActivity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.funput.funput.ui.kit.catalog.FunputCatalog
import app.funput.funput.speech.SpeechSpikeEditorActivity

/**
 * Debug-only entry to the FunputUI catalog, so the kit can be reviewed on a real device (real
 * glass, real fonts, real touch) before any screen is rebuilt on it. Not in release builds.
 */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("speech_spike", false)) {
            startActivity(Intent(this, SpeechSpikeEditorActivity::class.java))
            finish()
            return
        }
        if (intent.getBooleanExtra("speech_panel", false)) {
            startActivity(Intent(this, SpeechPanelCatalogActivity::class.java).putExtras(intent))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent { FunputCatalog() }
    }
}
