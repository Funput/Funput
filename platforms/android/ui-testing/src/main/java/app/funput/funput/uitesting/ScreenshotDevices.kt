package app.funput.funput.uitesting

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers

/**
 * Devices screenshot tests render on, as Robolectric qualifier strings.
 *
 * These are `const` so a test can name one in `@Config(qualifiers = ...)`, which only accepts
 * compile-time constants. The redesign targets current phones first; smaller and older screens
 * join this list when that work is picked up again.
 */
object ScreenshotDevices {
    /** A current flagship-sized phone (Pixel 8: 412×915dp at 420dpi). */
    const val PHONE: String = RobolectricDeviceQualifiers.Pixel8
}

/**
 * The Android API level screenshot tests run on, pinned so an upgrade is a deliberate diff.
 *
 * Not 36: Robolectric's API 36 runtime reaches into `jdk.internal.access`, which JDK 21 only
 * allows behind an `--add-opens` flag that every consuming module would have to repeat.
 */
const val SCREENSHOT_SDK: Int = 35
