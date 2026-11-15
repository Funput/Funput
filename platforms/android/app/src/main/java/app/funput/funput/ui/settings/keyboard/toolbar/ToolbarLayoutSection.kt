package app.funput.funput.ui.settings.keyboard.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import app.funput.funput.R
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.sectionTitle

/**
 * Ordered lists for the suggestion-bar sides: move a key up or down in its side, or send it to
 * the other side. No drag-and-drop in v1.
 */
@Composable
internal fun ToolbarLayoutSection(state: ToolbarLayoutSectionState) {
    val divider = @Composable { FunputDivider(startInset = FunputRowDefaults.IconDividerInset) }
    FunputSection(
        title = sectionTitle(R.string.settings_section_toolbar_layout),
        modifier = Modifier.testTag(ToolbarLayoutSettingsSectionTag),
    ) {
        BasicText(
            text = stringResource(R.string.settings_toolbar_layout_summary),
            style = FunputUi.typography.caption.copy(color = FunputUi.colors.secondaryLabel),
            modifier = Modifier.padding(horizontal = FunputUi.spacing.cardPadding, vertical = FunputUi.spacing.tight),
        )
        SideHeader(R.string.settings_toolbar_side_leading)
        SideSlots(state, ToolbarSide.LEADING, divider)
        divider()
        SideHeader(R.string.settings_toolbar_side_trailing)
        SideSlots(state, ToolbarSide.TRAILING, divider)
    }
}

@Composable
private fun SideHeader(title: Int) {
    BasicText(
        text = stringResource(title),
        style = FunputUi.typography.caption.copy(color = FunputUi.colors.secondaryLabel),
        modifier = Modifier.padding(horizontal = FunputUi.spacing.cardPadding, vertical = FunputUi.spacing.tight),
    )
}

@Composable
private fun SideSlots(
    state: ToolbarLayoutSectionState,
    side: ToolbarSide,
    divider: @Composable () -> Unit,
) {
    val slots = when (side) {
        ToolbarSide.LEADING -> state.preferences.leading
        ToolbarSide.TRAILING -> state.preferences.trailing
    }
    slots.forEachIndexed { index, slot ->
        if (index > 0) divider()
        ToolbarSlotRow(
            slot = slot,
            canMoveUp = index > 0,
            canMoveDown = index < slots.lastIndex,
            onMoveUp = { state.onMoveUp(side, index) },
            onMoveDown = { state.onMoveDown(side, index) },
            onMoveToOtherSide = { state.onMoveToOtherSide(side, index) },
            moveOtherLabel = stringResource(
                if (side == ToolbarSide.LEADING) R.string.keyboard_side_right
                else R.string.keyboard_side_left,
            ),
        )
    }
}

@Composable
private fun ToolbarSlotRow(
    slot: ToolbarSlot,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToOtherSide: () -> Unit,
    moveOtherLabel: String,
) {
    FunputRow(
        title = stringResource(ToolbarSlotLabels.title(slot)),
        icon = ToolbarSlotLabels.icon(slot),
        tint = FunputTint.BLUE,
        accessory = {
            Row(horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight), verticalAlignment = Alignment.CenterVertically) {
                TextAction(stringResource(R.string.settings_toolbar_move_up), canMoveUp, onMoveUp)
                TextAction(stringResource(R.string.settings_toolbar_move_down), canMoveDown, onMoveDown)
                TextAction(moveOtherLabel, true, onMoveToOtherSide)
            }
        },
    )
}

@Composable
private fun TextAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) FunputUi.colors.accent else FunputUi.colors.tertiaryLabel
    BasicText(
        text = label,
        style = FunputUi.typography.caption.copy(color = color),
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(FunputUi.spacing.tight),
    )
}

/** Test tag of the toolbar layout section. */
internal const val ToolbarLayoutSettingsSectionTag = "settings-section-toolbar-layout"
