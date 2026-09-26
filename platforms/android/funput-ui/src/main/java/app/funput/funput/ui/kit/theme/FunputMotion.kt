package app.funput.funput.ui.kit.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import app.funput.funput.ui.kit.tokens.MotionToken
import app.funput.funput.ui.kit.tokens.MotionTokens
import app.funput.funput.ui.kit.tokens.TokenCurve
import kotlin.math.PI
import kotlin.math.pow

/** Named animations of FunputUI, built from the motion tokens. */
object FunputMotion {
    /** Selecting something: a theme, a tab, a segment. */
    fun <T> selection(): AnimationSpec<T> = MotionTokens.themeSelect.toSpec()

    /** Something arriving with a little life: a sheet, a confirmation. */
    fun <T> emphasized(): AnimationSpec<T> = MotionTokens.launchBloom.toSpec()

    /** Something leaving. */
    fun <T> exit(): AnimationSpec<T> = MotionTokens.launchExit.toSpec()
}

/**
 * Translates a token, authored as SwiftUI's `spring(duration:bounce:)`, into Compose terms.
 *
 * SwiftUI defines a spring by its perceptual duration and bounce; Compose by damping ratio and
 * stiffness. For a unit-mass spring the period is `2π / √stiffness`, so a duration `d` gives
 * `stiffness = (2π / d)²`, and bounce 0..1 maps to damping `1 - bounce` (0 = critically damped).
 */
internal fun <T> MotionToken.toSpec(): AnimationSpec<T> = when (curve) {
    TokenCurve.SPRING -> spring(dampingRatio = dampingRatio(bounce), stiffness = stiffness(durationMillis))
    TokenCurve.EASE_OUT -> tween(durationMillis, easing = EaseOut)
    TokenCurve.EASE_IN_OUT -> tween(durationMillis, easing = EaseInOut)
    TokenCurve.LINEAR -> tween(durationMillis, easing = LinearEasing)
}

/** Damping ratio for a SwiftUI bounce; clamped so a bad token cannot make a spring unstable. */
internal fun dampingRatio(bounce: Float): Float = (1f - bounce).coerceIn(0.05f, 1f)

/** Stiffness whose natural period matches [durationMillis]. */
internal fun stiffness(durationMillis: Int): Float = (2 * PI / (durationMillis / 1000.0)).pow(2).toFloat()
