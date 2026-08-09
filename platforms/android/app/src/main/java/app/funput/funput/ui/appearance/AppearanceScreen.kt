package app.funput.funput.ui.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.theme.gallery.DeleteThemeDialog

/** Everything that decides how Funput looks: the app's own colours, then the keyboard's theme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceScreen(state: AppearanceScreenState, modifier: Modifier = Modifier) {
    var pendingDelete by remember { mutableStateOf<KeyboardThemeDescriptor?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_appearance)) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = state.onCreateTheme,
                modifier = Modifier.testTag(CreateThemeTag),
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.theme_gallery_create_title)) },
            )
        },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.Section),
            contentPadding = PaddingValues(
                start = Spacing.Large,
                end = Spacing.Large,
                top = padding.calculateTopPadding() + Spacing.Tight,
                // Room for the floating button to sit over the end of the list rather than on it.
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag(AppearanceListTag)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            item(key = "app") {
                AppAppearanceSection(
                    appearanceMode = state.appearanceMode,
                    dynamicColorEnabled = state.dynamicColorEnabled,
                    onAppearanceSelected = state.onAppearanceSelected,
                    onDynamicColorChanged = state.onDynamicColorChanged,
                )
            }
            item(key = "keyboard-mode") {
                KeyboardModeSection(
                    followsAppearance = state.followsAppearance,
                    activeSlot = state.activeSlot,
                    lightThemeName = state.lightThemeName,
                    darkThemeName = state.darkThemeName,
                    onFollowsAppearanceChange = state.onFollowsAppearanceChange,
                    onSlotSelected = state.onSlotSelected,
                )
            }
            themeSection(
                key = SystemThemesTag,
                titleRes = R.string.theme_gallery_system_section,
                themes = state.systemThemes,
                state = state,
                onRequestDelete = { pendingDelete = it },
            )
            themeSection(
                key = UserThemesTag,
                titleRes = R.string.theme_gallery_user_section,
                themes = state.userThemes,
                state = state,
                showsWhenEmpty = true,
                onRequestDelete = { pendingDelete = it },
            )
        }
    }
    pendingDelete?.let { descriptor ->
        DeleteThemeDialog(
            theme = descriptor,
            onConfirm = {
                state.onDeleteTheme(descriptor.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

internal const val AppearanceListTag = "appearance-list"
internal const val CreateThemeTag = "create-theme"
internal const val SystemThemesTag = "system-themes"
internal const val UserThemesTag = "user-themes"
