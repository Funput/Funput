package app.funput.funput.ime.editing.keyevent

import app.funput.funput.ime.editing.CompositionRenderMode

/**
 * Linux-sandbox / PC-framework packages that only accept KeyEvents.
 *
 * Prefixes, not product names. Add a constant here when a new OEM engine is confirmed;
 * split this object into `keyevent/hosts/` if a vendor needs its own notes.
 *
 * Huawei WPS, CAJViewer, and Edraw share [HUAWEI_PC_ENGINE] — matching the engine
 * covers every guest without naming each shortcut activity.
 */
internal object KeyEventHosts {
    private const val XIAOMI_WPS = "com.xiaomi.wps"
    private const val HUAWEI_PC_ENGINE = "com.huawei.hsl"
    private const val HUAWEI_WPS = "cn.wps.huawei"

    private val prefixes = listOf(XIAOMI_WPS, HUAWEI_PC_ENGINE, HUAWEI_WPS)

    fun matches(packageName: String?): Boolean =
        prefixes.any { packageName?.startsWith(it) == true }

    fun modeFor(packageName: String?): CompositionRenderMode? =
        CompositionRenderMode.KEY_EVENT.takeIf { matches(packageName) }
}
