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
 * Picks the move to animate from what actually changed.
 *
 * Sliding between tabs would claim they sit next to each other in an order the user is expected to
 * hold in their head; Material fades between them instead, and keeps sliding for going in and out
 * of a stack, where a direction means something.
 */
internal fun AnimatedContentTransitionScope<AppDestination>.destinationTransition(): ContentTransform =
    if (targetState.tab != initialState.tab) fadeThrough() else pushTransition()

/** Lateral: the outgoing tab drops away and the incoming one rises, with no direction implied. */
private fun fadeThrough(): ContentTransform =
    (fadeIn(animationSpec = tween(FadeMillis)) + scaleIn(initialScale = FadeThroughScale))
        .togetherWith(
            fadeOut(animationSpec = tween(FadeMillis)) + scaleOut(targetScale = FadeThroughScale),
        )

/**
 * In and out of a stack: the arriving screen slides in from the side it came from and the leaving
 * one settles back, so depth reads as depth.
 *
 * The slide springs, because that is the part a finger drags during a predictive back and a
 * duration-based curve would fight the gesture. Fade and scale stay on a short tween — seeking a
 * spring on opacity only makes the screen flicker.
 */
private fun AnimatedContentTransitionScope<AppDestination>.pushTransition(): ContentTransform {
    val forward = targetState.depth > initialState.depth
    val edge = if (forward) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    val enter = slideIntoContainer(edge, animationSpec = slideSpring()) +
        fadeIn(animationSpec = tween(FadeMillis)) +
        scaleIn(initialScale = if (forward) 1f else RestingScale)
    val exit = slideOutOfContainer(edge, animationSpec = slideSpring()) +
        fadeOut(animationSpec = tween(FadeMillis)) +
        scaleOut(targetScale = if (forward) RestingScale else 1f)
    return enter togetherWith exit
}

private fun <T> slideSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** How far the screen being left behind settles back. Material's own value for this move. */
private const val RestingScale = 0.9f
private const val FadeThroughScale = 0.94f
private const val FadeMillis = 120
