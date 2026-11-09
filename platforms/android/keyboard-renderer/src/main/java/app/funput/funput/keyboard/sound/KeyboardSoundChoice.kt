package app.funput.funput.keyboard.sound

/** The sound key presses make: a [style] played at a [volume]. */
data class KeyboardSoundChoice(
    val style: KeyboardSoundStyle = KeyboardSoundStyle.Default,
    val volume: KeyboardSoundVolume = KeyboardSoundVolume.Default,
) {
    companion object {
        /** The system click at medium volume, exactly as Funput sounded before the choice existed. */
        val Default = KeyboardSoundChoice()
    }
}
