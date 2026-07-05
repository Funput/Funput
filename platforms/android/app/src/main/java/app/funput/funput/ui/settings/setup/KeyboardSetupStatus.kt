package app.funput.funput.ui.settings.setup

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Android keyboard integration state for first-time setup guidance. */
enum class KeyboardSetupStatus {
    NOT_ENABLED,
    NOT_SELECTED,
    READY,
}

@Composable
internal fun rememberKeyboardSetupStatus(): KeyboardSetupStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(KeyboardSetupInspector.read(context)) }
    val refresh: () -> Unit = {
        status = KeyboardSetupInspector.read(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // IME picker is a system dialog; the activity often stays resumed while it is open.
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) refresh()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }

    DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        val settingsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                refresh()
            }
        }
        val resolver = context.contentResolver
        listOf(
            Settings.Secure.DEFAULT_INPUT_METHOD,
            Settings.Secure.ENABLED_INPUT_METHODS,
        ).forEach { key ->
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(key),
                false,
                settingsObserver,
            )
        }
        onDispose { resolver.unregisterContentObserver(settingsObserver) }
    }

    return status
}
