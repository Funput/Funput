package app.funput.funput.ui.kit.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.layout.FunputTab
import app.funput.funput.ui.kit.layout.FunputTabBar
import app.funput.funput.ui.kit.theme.FunputUiTheme

private val CatalogTabs = listOf(FunputTab("Nền tảng"), FunputTab("Hàng"), FunputTab("Điều khiển"))

/** The glass choices the catalog offers, in segment order; `null` means "this device's tier". */
internal val TierChoices: List<GlassTier?> = listOf(null, GlassTier.GLASS, GlassTier.SOLID)

/**
 * Every FunputUI component on one screen, to review the kit on a real device: switch tabs to see
 * each family, and switch the glass tier to compare real glass with the solid fallback over the
 * same content. The app exposes it from a debug-only launcher entry; it never ships in a release.
 */
@Composable
fun FunputCatalog(isDark: Boolean? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tierIndex by rememberSaveable { mutableIntStateOf(0) }
    // "Follow the device" keeps whatever tier is already in effect, so an outer override (tests pin
    // SOLID) survives; only an explicit choice replaces it.
    val tier = TierChoices[tierIndex] ?: LocalGlassTier.current
    val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalGlassTier provides tier) {
            FunputScreen(
                title = "FunputUI",
                tabBar = { FunputTabBar(CatalogTabs, selectedIndex = tab, onSelect = { tab = it }) },
            ) {
                when (tab) {
                    0 -> foundationSections(tierIndex = tierIndex, onTierSelected = { tierIndex = it })
                    1 -> rowSections()
                    else -> controlSections()
                }
            }
        }
    }
    if (isDark == null) FunputUiTheme(content = content) else FunputUiTheme(isDark = isDark, content = content)
}
