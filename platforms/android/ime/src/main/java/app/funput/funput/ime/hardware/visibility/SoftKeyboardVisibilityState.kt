package app.funput.funput.ime.hardware.visibility

import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences

/** What a hotkey press asks the service to do with the soft keyboard. */
internal enum class SoftKeyboardRequest { SHOW, HIDE }

/**
 * Decides whether the soft keyboard stays on screen while a physical keyboard is attached.
 *
 * The persisted setting is the baseline. The Alt+Space hotkey overrides it until the hotkey is
 * pressed again, the physical keyboard goes away, or the setting itself changes — a setting the
 * user just touched must not lose to a flip they made earlier.
 */
internal class SoftKeyboardVisibilityState {
    private var preferences = HardwareKeyboardPreferences.Default
    private var hotkeyOverride: Boolean? = null

    val showsWithHardwareKeyboard: Boolean
        get() = hotkeyOverride ?: preferences.showsSoftKeyboard

    val hotkeyEnabled: Boolean
        get() = preferences.toggleHotkeyEnabled

    fun update(value: HardwareKeyboardPreferences) {
        if (value == preferences) return
        preferences = value
        hotkeyOverride = null
    }

    /** Flips what the user currently sees, not the setting: the hotkey is a view toggle. */
    fun toggle(currentlyShown: Boolean): SoftKeyboardRequest {
        val show = !currentlyShown
        hotkeyOverride = show
        return if (show) SoftKeyboardRequest.SHOW else SoftKeyboardRequest.HIDE
    }

    fun onKeyboardDetached() {
        hotkeyOverride = null
    }
}
