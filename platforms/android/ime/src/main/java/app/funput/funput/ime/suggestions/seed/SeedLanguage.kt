package app.funput.funput.ime.suggestions

import app.funput.funput.ime.settings.ToneStyle

/**
 * What the shipped Vietnamese pairs may say: nothing, or predictions spelled in the
 * user's tone style. Mirrors `funput_suggestions::SeedLanguage`; [wireCode] is a copy
 * of its `WIRE_*` codes, which `nativeSetSeedLanguage` decodes.
 */
internal sealed interface SeedLanguage {
    val wireCode: Int

    /** The tone byte sent with [wireCode]; the composer's own value. */
    val toneStyleValue: Int

    /** English, a field that does not compose (password, URL, number), or not yet known. */
    data object Off : SeedLanguage {
        override val wireCode = 0
        /** Ignored by the engine while off; any valid byte will do. */
        override val toneStyleValue = ToneStyle.MODERN.nativeValue
    }

    data class Vietnamese(val toneStyle: ToneStyle) : SeedLanguage {
        override val wireCode = 1
        override val toneStyleValue get() = toneStyle.nativeValue
    }

    companion object {
        /** What the composer's state means for the shipped data. */
        fun of(composesVietnamese: Boolean, toneStyle: ToneStyle): SeedLanguage =
            if (composesVietnamese) Vietnamese(toneStyle) else Off
    }
}
