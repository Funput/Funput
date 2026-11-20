package app.funput.funput.keyboard.rendering.clipboard

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import app.funput.funput.keyboard.KeyboardClipboardOffer
import app.funput.funput.keyboard.chipLabel
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.rendering.RenderMetrics
import app.funput.funput.keyboard.suggestion.ClipboardOfferChip
import app.funput.funput.theme.KeyboardTheme

/**
 * Lays out and draws the content of the paste chip: the clipboard glyph and one line of text.
 *
 * The plate behind them is a [ResolvedKey], so the caller paints it with the key painter and the
 * chip matches the toolbar keys beside it in every surface style, flat or glass.
 */
internal class ClipboardOfferChipRenderer(
    private val resources: Resources,
    private val metrics: RenderMetrics,
) {
    private val glyph = ClipboardKeyIconRenderer(metrics)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val fontMetrics = Paint.FontMetrics()

    fun updateTheme(theme: KeyboardTheme) {
        glyph.updateTheme(theme)
        labelPaint.color = theme.specialLabelColor
    }

    /** The chip's plate on [band], sized to the glyph cell, the label and the padding after it. */
    fun plate(band: KeyBounds, offer: KeyboardClipboardOffer): ResolvedKey {
        labelPaint.textSize = metrics.sp(LabelSizeSp)
        val content = band.height + labelPaint.measureText(offer.chipLabel(resources)) + metrics.dp(EndPaddingDp)
        return ResolvedKey(PlateSpec, ClipboardOfferChip.plateBounds(band, content))
    }

    fun draw(canvas: Canvas, plate: ResolvedKey, offer: KeyboardClipboardOffer) {
        val bounds = plate.bounds
        // The glyph gets a square cell at the start, the size of a toolbar key.
        val cell = KeyBounds(bounds.left, bounds.top, bounds.left + bounds.height, bounds.bottom)
        glyph.draw(canvas, plate.copy(bounds = cell), offerActive = false)
        val available = bounds.right - cell.right - metrics.dp(EndPaddingDp)
        if (available <= 0f) return
        labelPaint.textSize = metrics.sp(LabelSizeSp)
        val label = TextUtils.ellipsize(offer.chipLabel(resources), labelPaint, available, TextUtils.TruncateAt.END)
        labelPaint.getFontMetrics(fontMetrics)
        val baseline = bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, 0, label.length, cell.right, baseline, labelPaint)
    }

    private companion object {
        const val LabelSizeSp = 14f
        const val EndPaddingDp = 12f

        /** Painted as a toolbar key; it is never part of a layout, so it carries no label. */
        val PlateSpec = KeySpec(ClipboardOfferChip.TargetId, "", KeyRole.CLIPBOARD, accessibilityLabel = "Dán")
    }
}
