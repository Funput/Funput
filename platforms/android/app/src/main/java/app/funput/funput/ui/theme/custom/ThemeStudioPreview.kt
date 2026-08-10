package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.navigation.sharedElementByKey
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.themePreviewSharedKey

/**
 * The live keyboard, with nothing around it.
 *
 * It used to sit inside a tinted card carrying a title and a subtitle explaining that it was a
 * preview — chrome around chrome, costing roughly a third of the screen to say something the
 * picture already says.
 */
@Composable
internal fun ThemeStudioPreview(
    theme: KeyboardTheme,
    backgroundImage: KeyboardThemeBackgroundImage?,
    editingThemeId: KeyboardThemeId?,
    modifier: Modifier = Modifier,
) {
    KeyboardThemePreview(
        theme = theme,
        backgroundImage = backgroundImage,
        modifier = modifier
            .sharedElementByKey(themePreviewSharedKey(editingThemeId))
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(CardShape),
    )
}

private val PreviewHeight = 178.dp
