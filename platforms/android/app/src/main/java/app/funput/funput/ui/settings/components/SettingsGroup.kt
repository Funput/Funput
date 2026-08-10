package app.funput.funput.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One row of a [SettingsGroup]. The group tells it where it sits so it can shape its corners. */
internal typealias SettingsRowContent = @Composable (RowPosition) -> Unit

/**
 * A group of settings rows.
 *
 * Rows are separate surfaces with a hairline between them rather than one slab cut by dividers.
 * That is what lets each row round its own corners and answer a press on its own — see
 * [rememberRowShape] — and it is the difference between a list that looks Material and a list that
 * looks ported from somewhere else.
 *
 * Rows arrive as a list rather than as a content lambda so the group can count them: positions are
 * derived, never passed by hand, so a row hidden behind a condition cannot leave a stale corner.
 */
@Composable
internal fun SettingsGroup(rows: List<SettingsRowContent>, modifier: Modifier = Modifier) {
    val positions = rowPositions(rows.size)
    Column(
        verticalArrangement = Arrangement.spacedBy(RowGap),
        modifier = modifier.fillMaxWidth(),
    ) {
        rows.forEachIndexed { index, row -> row(positions[index]) }
    }
}

private val RowGap = 2.dp
