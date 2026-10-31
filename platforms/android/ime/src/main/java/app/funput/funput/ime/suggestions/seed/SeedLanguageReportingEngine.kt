package app.funput.funput.ime.suggestions

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.ToneStyle

/**
 * The document's composer, reporting the state the shipped suggestion data must follow.
 *
 * The suggestion engine keeps its own copy of the input language and tone style, apart
 * from the composer's. Both reach the composer through this one object — the tone style
 * in [configure], Vietnamese on or off in [setEnabled] — so every cause of change (the
 * language key, a field that does not compose, a settings change) is reported from the
 * same place, and the two engines cannot disagree. Nothing is reported until both halves
 * are known, and nothing twice.
 */
internal class SeedLanguageReportingEngine(
    private val inner: VietnameseEngine,
    private val report: (SeedLanguage) -> Unit,
) : VietnameseEngine by inner {
    private var composesVietnamese: Boolean? = null
    private var toneStyle: ToneStyle? = null
    /** The last report, null until both halves are known. */
    var reported: SeedLanguage? = null
        private set

    override fun configure(configuration: EngineConfiguration) {
        inner.configure(configuration)
        toneStyle = configuration.toneStyle
        reportIfChanged()
    }

    override fun setEnabled(enabled: Boolean) {
        inner.setEnabled(enabled)
        composesVietnamese = enabled
        reportIfChanged()
    }

    private fun reportIfChanged() {
        val enabled = composesVietnamese ?: return
        val style = toneStyle ?: return
        val language = SeedLanguage.of(enabled, style)
        if (language == reported) return
        reported = language
        report(language)
    }
}
