package app.funput.funput.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.funput.funput.ui.kit.catalog.FunputCatalog

/**
 * Debug-only entry to the FunputUI catalog, so the kit can be reviewed on a real device (real
 * glass, real fonts, real touch) before any screen is rebuilt on it. Not in release builds.
 */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FunputCatalog() }
    }
}
