package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin

/** Stateless gallery for locally available keyboard themes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeGalleryScreen(
    themes: List<KeyboardThemeDescriptor>,
    selectedThemeId: KeyboardThemeId,
    followsAppearance: Boolean,
    activeSlot: KeyboardThemeSlot,
    onThemeSelected: (KeyboardThemeId) -> Unit,
    onFollowsAppearanceChange: (Boolean) -> Unit,
    onSlotSelected: (KeyboardThemeSlot) -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (KeyboardThemeId) -> Unit,
    onDeleteTheme: (KeyboardThemeId) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteTheme by remember { mutableStateOf<KeyboardThemeDescriptor?>(null) }
    val systemThemes = remember(themes) {
        themes.filter { descriptor -> descriptor.origin == KeyboardThemeOrigin.BUILT_IN }
    }
    val userThemes = remember(themes) {
        themes.filter { descriptor -> descriptor.origin != KeyboardThemeOrigin.BUILT_IN }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_gallery_title)) },
                // Null once the gallery is a tab's own root: there is nothing above it to go
                // back to, and an arrow that only leaves the tab would be lying about where it goes.
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.theme_gallery_back_description),
                            )
                        }
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            item(key = "description") {
                Text(
                    text = stringResource(R.string.theme_gallery_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item(key = "appearance") {
                ThemeAppearanceControls(
                    followsAppearance = followsAppearance,
                    activeSlot = activeSlot,
                    onFollowsAppearanceChange = onFollowsAppearanceChange,
                    onSlotSelected = onSlotSelected,
                )
            }
            item(key = "system-themes") {
                ThemeSection(
                    title = stringResource(R.string.theme_gallery_system_section),
                    themes = systemThemes,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                    onEditTheme = onEditTheme,
                    onDeleteTheme = { pendingDeleteTheme = it },
                    testTag = SystemThemesTag,
                )
            }
            item(key = "user-themes") {
                ThemeSection(
                    title = stringResource(R.string.theme_gallery_user_section),
                    themes = userThemes,
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected,
                    onEditTheme = onEditTheme,
                    onDeleteTheme = { pendingDeleteTheme = it },
                    testTag = UserThemesTag,
                    leadingContent = { cardModifier ->
                        CreateThemeCard(
                            onClick = onCreateTheme,
                            modifier = cardModifier,
                        )
                    },
                )
            }
        }
    }
    pendingDeleteTheme?.let { theme ->
        DeleteThemeDialog(
            theme = theme,
            onConfirm = {
                pendingDeleteTheme = null
                onDeleteTheme(theme.id)
            },
            onDismiss = { pendingDeleteTheme = null },
        )
    }
}

internal const val SystemThemesTag = "system-themes"
internal const val UserThemesTag = "user-themes"
