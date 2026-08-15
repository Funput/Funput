package app.funput.funput.keyboard

import android.content.res.Configuration

/**
 * True when this is a phone screen rotated to landscape — the case with by far the least height
 * to spend on a keyboard. Tablets are told apart by the same `sw600dp` breakpoint Android itself
 * uses, since they keep enough height to spare even sideways.
 */
internal val Configuration.isPhoneLandscape: Boolean
    get() = orientation == Configuration.ORIENTATION_LANDSCAPE && smallestScreenWidthDp < 600
