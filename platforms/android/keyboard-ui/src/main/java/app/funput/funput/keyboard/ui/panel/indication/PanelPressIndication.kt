package app.funput.funput.keyboard.ui.panel.indication

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Press feedback for everything tappable in a keyboard panel.
 *
 * Without one, Compose falls back to a debug indication that paints a black rectangle, which is
 * wrong on every theme. This washes the pressed element with [color] instead; a panel that has
 * no theme yet gets a neutral grey that shows on light and dark backgrounds alike.
 */
internal data class PanelPressIndication(private val color: Int?) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressOverlayNode(interactionSource, Color(color ?: NeutralPress))
}

private class PressOverlayNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    // Two fingers can hold the same element, so count presses rather than toggle a flag.
    private var presses = 0

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val wasPressed = presses > 0
                when (interaction) {
                    is PressInteraction.Press -> presses += 1
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        presses = (presses - 1).coerceAtLeast(0)
                }
                if (wasPressed != presses > 0) invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (presses > 0) drawRoundRect(color, cornerRadius = CornerRadius(PressCornerRadius.toPx()))
    }
}

private val PressCornerRadius = 8.dp
private const val NeutralPress = 0x29808080
