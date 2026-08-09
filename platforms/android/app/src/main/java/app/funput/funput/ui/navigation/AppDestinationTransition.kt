package app.funput.funput.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith

/**
 * How one destination gives way to the next: the arriving screen slides in from the side it came
 * from and the leaving one settles back a little, so depth reads as depth rather than as a cut.
 *
 * The slide springs, because that is the part a finger is dragging during a predictive back and a
 * duration-based curve would fight the gesture. Fade and scale stay on a short tween — seeking a
 * spring on opacity only makes the screen flicker.
 */
internal fun AnimatedContentTransitionScope<AppDestination>.destinationTransition(
    forward: Boolean,
): ContentTransform {
    val enterEdge = if (forward) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    val enter = slideIntoContainer(enterEdge, animationSpec = SlideSpring()) +
        fadeIn(animationSpec = tween(FadeMillis))
    val exit = slideOutOfContainer(enterEdge, animationSpec = SlideSpring()) +
        fadeOut(animationSpec = tween(FadeMillis)) +
        if (forward) scaleOut(targetScale = RestingScale) else scaleOut(targetScale = 1f)
    return (enter + if (forward) scaleIn(initialScale = 1f) else scaleIn(initialScale = RestingScale))
        .togetherWith(exit)
}

private fun <T> SlideSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** How far the screen being left behind settles back. Material's own value for this move. */
private const val RestingScale = 0.9f
private const val FadeMillis = 120
