package app.funput.funput.ui.settings.extraonsets

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter

/** Localized examples live in the app; the IME's onset model has no UI dependencies. */
@get:StringRes
internal val ExtraOnsetLetter.exampleResource: Int
    get() = when (this) {
        ExtraOnsetLetter.Z -> R.string.extra_onsets_example_z
        ExtraOnsetLetter.F -> R.string.extra_onsets_example_f
        ExtraOnsetLetter.W -> R.string.extra_onsets_example_w
        ExtraOnsetLetter.J -> R.string.extra_onsets_example_j
    }
