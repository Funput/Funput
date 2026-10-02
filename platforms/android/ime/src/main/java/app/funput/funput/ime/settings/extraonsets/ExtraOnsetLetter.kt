package app.funput.funput.ime.settings.extraonsets

/** An optional initial consonant; composition itself remains in the Rust engine. */
sealed class ExtraOnsetLetter private constructor(
    /** Lowercase letter used in persisted configuration. */
    val spelling: Char,
) {
    /** The onset in `zô` and `zui`. */
    data object Z : ExtraOnsetLetter('z')
    /** The onset in `fải` and `fan`. */
    data object F : ExtraOnsetLetter('f')
    /** The onset in `wá` and `wê`. */
    data object W : ExtraOnsetLetter('w')
    /** The onset in `jờ` and `Cư Jút`. */
    data object J : ExtraOnsetLetter('j')

    /** Canonical display and persistence order. */
    companion object {
        /** All supported letters, in the same order as the other platforms. */
        val Ordered: List<ExtraOnsetLetter> = listOf(Z, F, W, J)
    }
}
