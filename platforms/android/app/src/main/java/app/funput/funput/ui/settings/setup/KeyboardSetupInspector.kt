package app.funput.funput.ui.settings.setup

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import app.funput.funput.ime.FunputImeComponent

internal object KeyboardSetupInspector {
    fun read(context: Context): KeyboardSetupStatus {
        val imm = context.getSystemService(InputMethodManager::class.java)
        val enabledFromSystem = imm.enabledInputMethodList.any { it.packageName == FunputImeComponent.PACKAGE }
        // ENABLED_INPUT_METHODS is not readable for targetSdk > 33 (throws SecurityException);
        // the enabled-list API above already answers "is Funput enabled". DEFAULT_INPUT_METHOD is
        // read defensively so a platform/OEM restriction degrades to NOT_SELECTED, never a crash.
        val defaultSetting = readSecureSetting(context, Settings.Secure.DEFAULT_INPUT_METHOD)
        return resolve(
            enabledFromSystem = enabledFromSystem,
            enabledSetting = null,
            defaultSetting = defaultSetting,
        )
    }

    private fun readSecureSetting(context: Context, key: String): String? = try {
        Settings.Secure.getString(context.contentResolver, key)
    } catch (error: SecurityException) {
        null
    }

    fun resolve(
        enabledFromSystem: Boolean = false,
        enabledSetting: String?,
        defaultSetting: String?,
    ): KeyboardSetupStatus {
        if (!enabledFromSystem && !isImeEnabledInSettings(enabledSetting)) {
            return KeyboardSetupStatus.NOT_ENABLED
        }
        if (!isDefaultIme(defaultSetting)) return KeyboardSetupStatus.NOT_SELECTED
        return KeyboardSetupStatus.READY
    }

    /** Legacy pure resolver for unit tests that only have Secure settings strings. */
    fun resolve(enabledSetting: String?, defaultSetting: String?): KeyboardSetupStatus = resolve(
        enabledFromSystem = false,
        enabledSetting = enabledSetting,
        defaultSetting = defaultSetting,
    )

    private fun isImeEnabledInSettings(enabled: String?): Boolean {
        if (enabled.isNullOrBlank()) return false
        return enabled.split(';').any(FunputImeComponent::matches)
    }

    private fun isDefaultIme(default: String?): Boolean = FunputImeComponent.matches(default)

    /** Strips subtype suffix (`:123`) that Android appends to enabled/default IME entries. */
    internal fun imeComponentId(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().substringBefore(':')
    }
}
