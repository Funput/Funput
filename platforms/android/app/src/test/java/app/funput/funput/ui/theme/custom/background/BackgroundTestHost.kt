package app.funput.funput.ui.theme.custom.background

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.theme.custom.draft.ThemeDraftState
import app.funput.funput.ui.theme.custom.draft.rememberThemeDraftState
import app.funput.funput.ui.theme.custom.studio.studioBaseThemes
import java.io.File
import org.robolectric.RuntimeEnvironment

/** The background screen over a fresh draft, handed back through [onState] so tests can inspect it. */
@Composable
internal fun BackgroundTestHost(
    isDark: Boolean = false,
    imagePath: String? = null,
    onChooseImage: () -> Unit = {},
    onState: (ThemeDraftState) -> Unit = {},
) {
    FunputUiTheme(isDark = isDark) {
        CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
            val state = rememberThemeDraftState(studioBaseThemes, null)
            // Once, as a user would: removing the image later must not bring it back.
            LaunchedEffect(state) { imagePath?.let(state::selectBackgroundImage) }
            onState(state)
            ThemeBackgroundScreen(state = state, onChooseImage = onChooseImage, onBack = {})
        }
    }
}

/** A deterministic diagonal gradient saved as a PNG, standing in for a user's photo. */
internal fun testBackgroundImage(): String {
    val bitmap = Bitmap.createBitmap(Width, Height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, Width.toFloat(), Height.toFloat(), Start, End, Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, Width.toFloat(), Height.toFloat(), paint)
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "background-test.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return file.absolutePath
}

private const val Width = 480
private const val Height = 240
private const val Start = 0xFF3A7BD5.toInt()
private const val End = 0xFFF59E0B.toInt()
