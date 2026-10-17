package app.funput.funput.ui.settings.hub

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.keyboard.KeyboardPreviewCard
import app.funput.funput.ui.settings.keyboard.KeyboardSetupCard
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

/**
 * The keyboard hub, top to bottom: the keyboard preview, setup while it is unfinished, then every
 * settings page as one card of rows.
 */
internal fun LazyListScope.keyboardHubItems(
    state: SettingsScreenState,
    rows: List<SettingsHubRow>,
    onOpenPage: (SettingsPage) -> Unit,
) {
    item(key = "preview") {
        KeyboardPreviewCard(
            descriptor = state.keyboardTheme,
            inputMethod = state.inputMethod,
            sizingProfile = state.keySizeProfile,
            showsNumberRow = state.showsNumberRow,
            onOpenAppearance = state.onOpenAppearance,
            modifier = Modifier.testTag(SettingsHeroTag),
        )
    }
    if (state.keyboardSetupStatus != KeyboardSetupStatus.READY) {
        item(key = "setup") {
            KeyboardSetupCard(
                status = state.keyboardSetupStatus,
                onEnableKeyboard = state.onEnableKeyboard,
                onSelectKeyboard = state.onSelectKeyboard,
                modifier = Modifier.testTag(SettingsSetupTag),
            )
        }
    }
    item(key = "all") {
        SettingsNavigationCard(
            title = stringResource(R.string.settings_all_title),
            rows = rows,
            onOpenPage = onOpenPage,
            modifier = Modifier.testTag(SettingsAllTag),
        )
    }
}

/** Test tag of the keyboard preview card. */
internal const val SettingsHeroTag = "settings-hero"

/** Test tag of the setup card. */
internal const val SettingsSetupTag = "settings-setup"

/** Test tag of the card listing every settings page. */
internal const val SettingsAllTag = "settings-all"
