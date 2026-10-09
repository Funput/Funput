package app.funput.funput.ui.kit.overlays

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A question that needs an answer before going on: a title, an optional [message], a confirm
 * action and an optional dismiss action. Set [destructive] when confirming deletes or resets
 * something, so the button says so in colour.
 *
 * Material's dialog, restyled, for its focus handling, back behaviour and accessibility.
 */
@Composable
fun FunputDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            FunputButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) FunputButtonStyle.DESTRUCTIVE else FunputButtonStyle.PLAIN,
            )
        },
        dismissButton = dismissLabel?.let { label ->
            { FunputButton(text = label, onClick = onDismiss, style = FunputButtonStyle.PLAIN) }
        },
        title = { BasicText(title, style = type.title.copy(color = colors.label)) },
        text = message?.let { { BasicText(it, style = type.body.copy(color = colors.secondaryLabel)) } },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(28.dp),
    )
}
