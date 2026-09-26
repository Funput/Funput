package app.funput.funput.ime

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Opens the installed app from the IME without depending on the app module. */
internal class ImeSettingsLauncher(
    private val finishComposition: () -> Unit,
    private val launch: () -> Boolean,
    private val showFailure: () -> Unit,
) {
    constructor(context: Context, finishComposition: () -> Unit) : this(
        finishComposition = finishComposition,
        launch = {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent == null) false else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        },
        showFailure = { Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_SHORT).show() },
    )

    fun open() {
        finishComposition()
        val opened = try {
            launch()
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
        if (!opened) showFailure()
    }
}
