package app.funput.funput.keyboard.model

data class KeySpec(
    val id: String,
    val label: String,
    val role: KeyRole,
    val widthWeight: Float = 1f,
    val shiftedLabel: String? = null,
    val secondaryLabel: String? = null,
    val accessibilityLabel: String = label,
    val horizontalSwipeAction: KeySwipeAction? = null,
) {
    init {
        require(id.isNotBlank()) { "Key id must not be blank" }
        require(widthWeight > 0f) { "Key width weight must be positive" }
        require(shiftedLabel == null || shiftedLabel.isNotEmpty()) { "Shifted label must not be empty" }
        require(accessibilityLabel.isNotBlank()) { "Accessibility label must not be blank" }
    }
}

enum class KeySwipeAction {
    TOGGLE_LANGUAGE,
}

data class KeyboardRow(
    val keys: List<KeySpec>,
    val horizontalInsetUnits: Float = 0f,
) {
    init {
        require(keys.isNotEmpty()) { "Keyboard row must contain at least one key" }
        require(horizontalInsetUnits >= 0f) { "Row inset must not be negative" }
        require(keys.map(KeySpec::id).distinct().size == keys.size) { "Key ids must be unique within a row" }
    }
}

data class SuggestionBarSpec(
    val settingsKey: KeySpec,
    val emojiKey: KeySpec,
    val suggestionsEnabled: Boolean = true,
) {
    init {
        require(settingsKey.role == KeyRole.SETTINGS) { "Toolbar settings action must be a settings key" }
        require(emojiKey.role == KeyRole.EMOJI) { "Toolbar emoji action must be an emoji key" }
    }
}

data class KeyboardLayout(
    val id: String,
    val inputMethod: KeyboardInputMethod,
    val suggestionBar: SuggestionBarSpec?,
    val rows: List<KeyboardRow>,
) {
    init {
        require(id.isNotBlank()) { "Layout id must not be blank" }
        require(rows.isNotEmpty()) { "Keyboard layout must contain at least one row" }

        val keyIds = buildList {
            suggestionBar?.settingsKey?.let { add(it.id) }
            suggestionBar?.emojiKey?.let { add(it.id) }
            rows.forEach { row -> addAll(row.keys.map(KeySpec::id)) }
        }
        require(keyIds.distinct().size == keyIds.size) { "Key ids must be unique within a layout" }
    }
}
