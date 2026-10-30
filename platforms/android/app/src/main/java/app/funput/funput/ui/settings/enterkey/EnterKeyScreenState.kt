package app.funput.funput.ui.settings.enterkey

import androidx.compose.runtime.Immutable

/**
 * One app whose Enter key sends. [app] is null while the launcher's apps load, and for good once
 * the app has been uninstalled, which [isMissing] tells apart.
 */
@Immutable
internal class ChosenApp(val packageName: String, val app: InstalledApp?, val isMissing: Boolean) {
    val label: String get() = app?.label ?: packageName
}

/**
 * What the Enter key page renders and every action it can dispatch.
 *
 * [packages] is null until the store has been read, so the page does not flash "nothing chosen".
 * [installed] is null while the launcher's apps are still being read; until then chosen apps show
 * by package name and nothing is suggested.
 */
@Immutable
internal class EnterKeyScreenState(
    packages: Set<String>?,
    installed: List<InstalledApp>?,
    val onAdd: (String) -> Unit,
    val onRemove: (String) -> Unit,
) {
    private val byPackage = installed.orEmpty().associateBy { it.packageName }

    val isLoaded: Boolean = packages != null

    /** Chosen apps by name, as the picker lists them; uninstalled ones last. */
    val chosen: List<ChosenApp> = packages.orEmpty()
        .map { ChosenApp(it, byPackage[it], isMissing = installed != null && it !in byPackage) }
        .sortedWith(compareBy<ChosenApp> { it.isMissing }.thenBy(InstalledApps.labelOrder) { it.label })

    /** Checked apps on this device that are not chosen yet. */
    val suggestions: List<InstalledApp> =
        InstalledApps.Suggested.filter { it !in packages.orEmpty() }.mapNotNull(byPackage::get)

    /** What the picker offers: every launcher app not chosen yet, or null while loading. */
    val available: List<InstalledApp>? = installed?.filter { it.packageName !in packages.orEmpty() }

    companion object {
        /** Nothing chosen and no apps read, with no-op actions, for previews and tests. */
        val Inert = EnterKeyScreenState(emptySet(), emptyList(), onAdd = {}, onRemove = {})
    }
}
