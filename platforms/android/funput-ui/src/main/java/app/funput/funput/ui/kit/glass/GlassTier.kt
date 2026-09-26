package app.funput.funput.ui.kit.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * How glass surfaces are drawn on this device.
 *
 * Two tiers, on purpose. The glass library blurs from API 31 and adds edge refraction on its own
 * from API 33, so one [GLASS] tier covers both; below 31 it would draw a near-transparent film,
 * which is why [SOLID] exists rather than letting the library degrade by itself.
 */
enum class GlassTier {
    /** Backdrop blur, and refraction where the platform supports it. */
    GLASS,

    /** A translucent card-coloured surface with a hairline; samples nothing behind it. */
    SOLID,
    ;

    companion object {
        /** First API level with `RenderEffect`, which backdrop blur needs. */
        const val MIN_GLASS_SDK: Int = Build.VERSION_CODES.S

        /** The tier for a device; low-RAM devices get [SOLID] because every glass layer costs memory. */
        fun resolve(sdkInt: Int, isLowRamDevice: Boolean): GlassTier =
            if (sdkInt >= MIN_GLASS_SDK && !isLowRamDevice) GLASS else SOLID
    }
}

/**
 * Overrides the device's tier for everything below it: the catalog uses it to compare tiers side
 * by side, and screenshot tests pin [GlassTier.SOLID] because JVM rendering cannot run shaders.
 * `null`, the default, means "resolve from the device".
 */
val LocalGlassTier: ProvidableCompositionLocal<GlassTier?> = compositionLocalOf { null }

/** The tier to draw with here: the [LocalGlassTier] override, or this device's own. */
@Composable
fun currentGlassTier(): GlassTier {
    LocalGlassTier.current?.let { return it }
    val context = LocalContext.current
    return remember(context) { GlassTier.resolve(Build.VERSION.SDK_INT, context.isLowRamDevice()) }
}

private fun Context.isLowRamDevice(): Boolean =
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice ?: false
