package app.funput.funput.ime.settings

data class KeyboardFeedbackPreferences(
    val hapticsEnabled: Boolean,
    val soundsEnabled: Boolean,
) {
    companion object {
        val Default = KeyboardFeedbackPreferences(
            hapticsEnabled = true,
            soundsEnabled = false,
        )
    }
}
