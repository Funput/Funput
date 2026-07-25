package app.funput.funput.keyboard.rendering

import app.funput.funput.keyboard.model.KeyRole

/**
 * Whether a key is a modifier rather than something the user is composing with.
 *
 * Special keys take the theme's recessive plate and label colors so the alphabet stays dominant.
 * Spacebar is deliberately excluded: it reads as part of the typing surface.
 */
internal val KeyRole.isSpecial: Boolean
    get() = when (this) {
        KeyRole.CHARACTER, KeyRole.VNI_MODIFIER, KeyRole.PUNCTUATION, KeyRole.SPACE -> false
        else -> true
    }
