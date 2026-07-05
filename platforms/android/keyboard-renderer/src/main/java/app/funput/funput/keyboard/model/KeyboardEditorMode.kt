package app.funput.funput.keyboard.model

/** Describes the focused editor so the renderer and IME can apply matching behavior. */
enum class KeyboardEditorMode(
    val supportsVietnameseComposition: Boolean,
    val isNumber: Boolean = false,
    val isPassword: Boolean = false,
    val allowsDecimal: Boolean = false,
    val allowsSigned: Boolean = false,
) {
    TEXT(supportsVietnameseComposition = true),
    EMAIL(supportsVietnameseComposition = false),
    URL(supportsVietnameseComposition = false),
    PHONE(supportsVietnameseComposition = false),
    PASSWORD(supportsVietnameseComposition = false, isPassword = true),
    PIN(supportsVietnameseComposition = false, isPassword = true),
    NUMBER(supportsVietnameseComposition = false, isNumber = true),
    NUMBER_DECIMAL(supportsVietnameseComposition = false, isNumber = true, allowsDecimal = true),
    NUMBER_SIGNED(supportsVietnameseComposition = false, isNumber = true, allowsSigned = true),
    NUMBER_SIGNED_DECIMAL(
        supportsVietnameseComposition = false,
        isNumber = true,
        allowsDecimal = true,
        allowsSigned = true,
    ),
    ;

    val usesKeypad: Boolean get() = isNumber || this == PHONE || this == PIN
}
