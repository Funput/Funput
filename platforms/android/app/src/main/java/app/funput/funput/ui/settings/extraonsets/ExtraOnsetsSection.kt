package app.funput.funput.ui.settings.extraonsets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi

/** Optional consonants use the same master-and-letter interaction as iOS. */
@Composable
internal fun ExtraOnsetsSection(state: ExtraOnsetsSectionState) {
    val warning = stringResource(R.string.extra_onsets_warning)
    val footer = if (state.showsAdvancedWHint) {
        warning + "\n\n" + stringResource(R.string.extra_onsets_advanced_w_hint)
    } else warning
    Column(Modifier.testTag(ExtraOnsetsSectionTag)) {
        FunputSection(title = stringResource(R.string.extra_onsets_section), footer = footer) {
            ToggleRow(
                title = stringResource(R.string.extra_onsets_enabled_title),
                summary = stringResource(R.string.extra_onsets_enabled_summary),
                checked = state.selection.isEnabled,
                onCheckedChange = state.onEnabledChanged,
                icon = FunputIcons.ToneMarks,
                tint = FunputTint.PURPLE,
                enabled = !state.isSaving,
                modifier = Modifier.testTag(ExtraOnsetsMasterTag),
            )
            // Compose's duration scale honors the system's disabled-animation setting.
            AnimatedVisibility(
                visible = state.selection.isEnabled,
                enter = fadeIn(FunputMotion.selection()) + expandVertically(FunputMotion.selection()),
                exit = fadeOut(FunputMotion.selection()) + shrinkVertically(FunputMotion.selection()),
            ) {
                Column {
                    ExtraOnsetLetter.Ordered.forEach { letter ->
                        FunputDivider()
                        ToggleRow(
                            title = stringResource(R.string.extra_onsets_letter_title, letter.spelling.toString()),
                            summary = stringResource(letter.exampleResource),
                            checked = letter in state.selection,
                            onCheckedChange = { enabled -> state.onLetterChanged(letter, enabled) },
                            enabled = !state.isSaving,
                            modifier = Modifier.testTag(extraOnsetLetterTag(letter)),
                        )
                    }
                }
            }
        }
        if (state.hasSaveError) {
            BasicText(
                text = stringResource(R.string.extra_onsets_save_error),
                style = FunputUi.typography.caption.copy(color = FunputUi.colors.secondaryLabel),
                modifier = Modifier
                    .padding(horizontal = FunputUi.spacing.cardPadding, vertical = FunputUi.spacing.small)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** Section tag for scroll and accessibility regression checks. */
internal const val ExtraOnsetsSectionTag = "settings-extra-onsets"

/** The master switch has its own tag, independent of localized labels. */
internal const val ExtraOnsetsMasterTag = "extra-onsets-master"

/** Stable switch identity for a supported letter. */
internal fun extraOnsetLetterTag(letter: ExtraOnsetLetter): String = "extra-onsets-${letter.spelling}"
