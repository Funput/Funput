package app.funput.funput.ime.settings

/** Tone-mark placement style. Native values match `funput_core::ToneStyle`. */
enum class ToneStyle(val nativeValue: Int) {
    TRADITIONAL(0),
    MODERN(1),
    ;

    companion object {
        fun fromNativeValue(value: Int): ToneStyle = entries.firstOrNull { it.nativeValue == value }
            ?: TRADITIONAL
    }
}
