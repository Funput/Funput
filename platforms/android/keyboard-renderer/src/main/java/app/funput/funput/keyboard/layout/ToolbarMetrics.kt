package app.funput.funput.keyboard.layout

/**
 * Density-independent toolbar geometry, shared by the layout, the renderer and the panel
 * height budget.
 *
 * One copy on purpose: [KeyboardDimensions][app.funput.funput.keyboard.KeyboardDimensions]
 * reserves this strip when it measures the panel and [KeyboardGeometry] lays the band out
 * inside it. Two copies that drift leave the keyboard either squeezing its rows or showing
 * a blank gap above them.
 */
internal object ToolbarMetrics {
    /**
     * The suggestion band, sized like Gboard's strip rather than like a key row: the toolbar
     * carries one line of text and two icons, so anything taller is keyboard height spent on
     * padding. The utility keys still take the full band and widen into their neighbours'
     * slack (see [KeyboardHitTargetResolver]), so the shorter band costs no tap area.
     */
    const val SuggestionBarHeightDp = 36f

    /** Separates the band from the first key row. */
    const val SuggestionBarGapDp = 4f

    /** The vertical strip the toolbar claims in total. */
    const val ChromeDp = SuggestionBarHeightDp + SuggestionBarGapDp

    /** The brand mark is decorative, so it sits inside the band instead of filling it. */
    const val LogoSizeRatio = 0.68f
}
