package app.funput.funput.ui.settings.enterkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputMessageCard
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputIconButton
import app.funput.funput.ui.kit.gestures.SwipeToDelete
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.enterkey.components.AppRow
import app.funput.funput.ui.settings.enterkey.components.NewLineSection

/**
 * "Phím Enter": the apps whose Enter key sends, apps worth adding, and how to break a line once
 * Enter sends. Adding goes through a sheet of the launcher's apps; removing asks first.
 */
@Composable
internal fun EnterKeyScreen(state: EnterKeyScreenState, onBack: () -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<ChosenApp?>(null) }
    FunputScreen(
        title = stringResource(R.string.settings_section_enter_key),
        onBack = onBack,
        actions = {
            FunputIconButton(
                icon = FunputIcons.Add,
                contentDescription = stringResource(R.string.settings_enter_key_add),
                onClick = { picking = true },
            )
        },
    ) {
        // One item for the list and its suggestions, so the set of items never changes as they load.
        item(key = "apps") {
            Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.section)) {
                ChosenApps(state, onAdd = { picking = true }, onRemove = { pendingRemoval = it })
                SuggestedApps(state)
            }
        }
        item(key = "new-line") { NewLineSection() }
    }
    if (picking) {
        AppPickerSheet(
            apps = state.available,
            onPick = { packageName -> state.onAdd(packageName); picking = false },
            onDismiss = { picking = false },
        )
    }
    pendingRemoval?.let { app ->
        FunputDialog(
            title = stringResource(R.string.settings_enter_key_remove_title, app.label),
            message = stringResource(R.string.settings_enter_key_remove_body, app.label),
            confirmLabel = stringResource(R.string.settings_enter_key_remove),
            onConfirm = { state.onRemove(app.packageName); pendingRemoval = null },
            onDismiss = { pendingRemoval = null },
            dismissLabel = stringResource(R.string.settings_enter_key_cancel),
            destructive = true,
        )
    }
}

@Composable
private fun ChosenApps(state: EnterKeyScreenState, onAdd: () -> Unit, onRemove: (ChosenApp) -> Unit) {
    if (!state.isLoaded) return
    if (state.chosen.isEmpty()) {
        FunputMessageCard(
            title = stringResource(R.string.settings_enter_key_empty_title),
            body = stringResource(R.string.settings_enter_key_empty_body),
            actionLabel = stringResource(R.string.settings_enter_key_add),
            onAction = onAdd,
        )
        return
    }
    FunputSection(
        title = stringResource(R.string.settings_enter_key_apps_title),
        footer = stringResource(R.string.settings_enter_key_apps_footer),
    ) {
        state.chosen.forEachIndexed { index, chosen ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            SwipeToDelete(
                label = stringResource(R.string.settings_enter_key_remove),
                onDelete = { onRemove(chosen) },
                modifier = Modifier.testTag("enter-key-app-${chosen.packageName}"),
            ) {
                AppRow(
                    packageName = chosen.packageName,
                    title = chosen.label,
                    summary = if (chosen.isMissing) stringResource(R.string.settings_enter_key_not_installed) else null,
                )
            }
        }
    }
}

@Composable
private fun SuggestedApps(state: EnterKeyScreenState) {
    if (state.suggestions.isEmpty()) return
    FunputSection(title = stringResource(R.string.settings_enter_key_suggestions_title)) {
        state.suggestions.forEachIndexed { index, app ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            val addDescription = stringResource(R.string.settings_enter_key_suggestion_add_description, app.label)
            AppRow(
                packageName = app.packageName,
                title = app.label,
                summary = stringResource(R.string.settings_enter_key_suggestion_summary),
                accessory = {
                    FunputButton(
                        text = stringResource(R.string.settings_enter_key_suggestion_add),
                        onClick = { state.onAdd(app.packageName) },
                        // "Thêm" alone does not say what is added once there is more than one row.
                        modifier = Modifier.semantics { contentDescription = addDescription },
                        style = FunputButtonStyle.SECONDARY,
                    )
                },
            )
        }
    }
}
