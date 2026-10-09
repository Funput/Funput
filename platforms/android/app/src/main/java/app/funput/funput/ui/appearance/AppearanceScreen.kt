package app.funput.funput.ui.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.appearance.gallery.DeleteThemeDialog
import app.funput.funput.ui.appearance.gallery.ThemeActionsSheet
import app.funput.funput.ui.kit.controls.FunputIconButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen

/**
 * Everything that decides how Funput looks: the app's light/dark mode, whether the keyboard keeps
 * one theme or one per mode, then the theme gallery. "Create" sits in the top bar; a custom
 * theme's edit and delete open from its card as a sheet, and deleting asks first.
 */
@Composable
internal fun AppearanceScreen(
    state: AppearanceScreenState,
    modifier: Modifier = Modifier,
    tabBar: (@Composable () -> Unit)? = null,
) {
    var actionsFor by remember { mutableStateOf<KeyboardThemeDescriptor?>(null) }
    var pendingDelete by remember { mutableStateOf<KeyboardThemeDescriptor?>(null) }
    FunputScreen(
        title = stringResource(R.string.nav_appearance),
        modifier = modifier,
        tabBar = tabBar,
        actions = {
            FunputIconButton(
                icon = FunputIcons.Add,
                contentDescription = stringResource(R.string.theme_gallery_create_title),
                onClick = state.onCreateTheme,
                modifier = Modifier.testTag(CreateThemeTag),
            )
        },
    ) {
        appearanceSections(state = state, onThemeActions = { actionsFor = it })
    }
    actionsFor?.let { descriptor ->
        ThemeActionsSheet(
            descriptor = descriptor,
            onEdit = {
                actionsFor = null
                state.onEditTheme(descriptor.id)
            },
            onDelete = {
                actionsFor = null
                pendingDelete = descriptor
            },
            onDismiss = { actionsFor = null },
        )
    }
    pendingDelete?.let { descriptor ->
        DeleteThemeDialog(
            theme = descriptor,
            onConfirm = {
                pendingDelete = null
                state.onDeleteTheme(descriptor.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Test tag of the top bar's "create a theme" button. */
internal const val CreateThemeTag = "create-theme"
