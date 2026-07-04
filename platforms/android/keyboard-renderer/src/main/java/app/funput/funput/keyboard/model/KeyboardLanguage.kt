package app.funput.funput.keyboard.model

enum class KeyboardLanguage(val displayLabel: String) {
    VIETNAMESE("Tiếng Việt"),
    ENGLISH("Tiếng Anh"),
    ;

    fun toggled(): KeyboardLanguage = when (this) {
        VIETNAMESE -> ENGLISH
        ENGLISH -> VIETNAMESE
    }
}
