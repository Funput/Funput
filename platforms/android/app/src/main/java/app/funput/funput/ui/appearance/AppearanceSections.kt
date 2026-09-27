package app.funput.funput.ui.appearance

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.ui.appearance.gallery.ThemeCard
import app.funput.funput.ui.appearance.gallery.ThemeEmptyState
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.cards.FunputSectionHeader
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.SegmentedRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.settings.label

/**
 * The page's content: the app's mode, the keyboard's one-or-two-themes switch (and, with two,
 * which slot the gallery assigns to), then built-in and custom themes, one full-width card each.
 */
internal fun LazyListScope.appearanceSections(
    state: AppearanceScreenState,
    onThemeActions: (KeyboardThemeDescriptor) -> Unit,
) {
    item(key = "app") { AppModeSection(state) }
    item(key = "keyboard-mode") { KeyboardModeSection(state) }
    themeSection(SystemThemesTag, R.string.theme_gallery_system_section, state.systemThemes, state, onThemeActions)
    themeSection(UserThemesTag, R.string.theme_gallery_user_section, state.userThemes, state, onThemeActions)
}

@Composable
private fun AppModeSection(state: AppearanceScreenState) {
    val modes = AppearanceMode.entries
    FunputSection(title = stringResource(R.string.appearance_section_app)) {
        SegmentedRow(
            title = stringResource(R.string.appearance_app_mode),
            options = modes.map { it.label() },
            selectedIndex = modes.indexOf(state.appearanceMode),
            onSelect = { index -> state.onAppearanceSelected(modes[index]) },
            icon = FunputIcons.LightDark,
        )
    }
}

/**
 * The slot choice names the theme in each slot, not just the slot: tapping a card means a
 * different thing depending on which is active, so the control has to say so where the eye is.
 */
@Composable
private fun KeyboardModeSection(state: AppearanceScreenState) {
    FunputSection(title = stringResource(R.string.appearance_section_keyboard)) {
        ToggleRow(
            title = stringResource(R.string.theme_gallery_follow_appearance),
            summary = stringResource(R.string.theme_gallery_follow_appearance_description),
            checked = state.followsAppearance,
            onCheckedChange = state.onFollowsAppearanceChange,
            icon = FunputIcons.Appearance,
        )
        if (state.followsAppearance) {
            val slots = listOf(KeyboardThemeSlot.LIGHT, KeyboardThemeSlot.DARK)
            FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            SegmentedRow(
                title = stringResource(R.string.appearance_slot_assigning),
                options = listOf(
                    stringResource(R.string.appearance_slot_light_named, state.lightThemeName),
                    stringResource(R.string.appearance_slot_dark_named, state.darkThemeName),
                ),
                selectedIndex = slots.indexOf(state.activeSlot).coerceAtLeast(0),
                onSelect = { index -> state.onSlotSelected(slots[index]) },
            )
        }
    }
}

private fun LazyListScope.themeSection(
    key: String,
    @StringRes titleRes: Int,
    themes: List<KeyboardThemeDescriptor>,
    state: AppearanceScreenState,
    onThemeActions: (KeyboardThemeDescriptor) -> Unit,
) {
    if (themes.isEmpty()) {
        item(key = "$key-empty") {
            Column {
                FunputSectionHeader(stringResource(titleRes))
                ThemeEmptyState(onCreate = state.onCreateTheme)
            }
        }
        return
    }
    // The header rides on the first card: the page spaces its items a section apart, which would
    // otherwise leave the title floating away from the group it names.
    itemsIndexed(items = themes, key = { _, descriptor -> "$key-${descriptor.id.value}" }) { index, descriptor ->
        Column {
            if (index == 0) FunputSectionHeader(stringResource(titleRes))
            ThemeCard(
                descriptor = descriptor,
                selected = descriptor.id == state.selectedThemeId,
                onSelected = { state.onThemeSelected(descriptor.id) },
                onMore = if (descriptor.origin == KeyboardThemeOrigin.CUSTOM) {
                    { onThemeActions(descriptor) }
                } else {
                    null
                },
            )
        }
    }
}

/** Key of the built-in themes' header. */
internal const val SystemThemesTag = "system-themes"

/** Key of the user's themes' header. */
internal const val UserThemesTag = "user-themes"
