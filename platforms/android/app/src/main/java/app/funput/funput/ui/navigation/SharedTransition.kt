package app.funput.funput.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The two scopes a shared element needs, published by [AppNavDisplay] for the screens under it.
 *
 * They travel as a composition local rather than as parameters because the elements that want them
 * — a card inside a section inside a gallery — sit several layers below the navigation host, and
 * threading an animation scope through every one of those signatures would say nothing about what
 * those functions do.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Immutable
internal class SharedTransitionScopes(
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
)

/** Null wherever no navigation host is above, which is how previews and UI tests see it. */
internal val LocalSharedTransitionScopes = compositionLocalOf<SharedTransitionScopes?> { null }

/**
 * Matches this element with the one carrying the same [key] on the destination being animated to,
 * so the two are drawn as one thing moving rather than two things crossfading.
 *
 * A null [key], or no navigation host above, leaves the modifier untouched: a theme being created
 * from scratch has no card to grow out of, and neither does a preview.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.sharedElementByKey(key: Any?): Modifier {
    val scopes = LocalSharedTransitionScopes.current
    if (key == null || scopes == null) return this
    with(scopes.sharedTransitionScope) {
        return this@sharedElementByKey.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = scopes.animatedVisibilityScope,
        )
    }
}
