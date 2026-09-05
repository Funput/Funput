package app.funput.funput.ime.editing.keyevent

import app.funput.funput.ime.editing.CompositionCompatibilityPolicy
import app.funput.funput.ime.editing.CompositionRenderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyEventHostsTest {
    @Test
    fun sandboxEnginesSelectKeyEventMode() {
        listOf(
            "com.xiaomi.wpslauncher",
            "com.huawei.hsl",
            "cn.wps.huawei",
        ).forEach { packageName ->
            assertTrue(packageName, KeyEventHosts.matches(packageName))
            assertEquals(
                packageName,
                CompositionRenderMode.KEY_EVENT,
                CompositionCompatibilityPolicy.renderMode(packageName),
            )
        }
    }

    @Test
    fun nativeWpsAndOtherHuaweiAppsStayOffThisHandle() {
        listOf(
            "cn.wps.moffice_eng",
            "cn.wps.moffice",
            "com.huawei.android.launcher",
        ).forEach { packageName ->
            assertFalse(packageName, KeyEventHosts.matches(packageName))
        }
    }
}
