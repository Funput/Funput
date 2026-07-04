package app.funput.funput.keyboard.rendering

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.theme.KeyboardTheme

/** Draws a fully resolved keyboard without owning Android view state. */
internal class KeyboardCanvasRenderer(resources: Resources) {
    private val metrics = RenderMetrics(resources)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyRenderer = KeyRenderer(metrics)
    private val suggestionBarRenderer = SuggestionBarRenderer(metrics)
    private var theme: KeyboardTheme = KeyboardTheme.Aurora

    fun updateTheme(theme: KeyboardTheme, width: Int, height: Int) {
        this.theme = theme
        keyRenderer.updateTheme(theme)
        suggestionBarRenderer.updateTheme(theme)
        if (width > 0 && height > 0) {
            backgroundPaint.shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                theme.backgroundStartColor,
                theme.backgroundEndColor,
                Shader.TileMode.CLAMP,
            )
        }
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        keyboard: ResolvedKeyboard,
        suggestions: List<String>,
        pressedKeys: PressedKeyState,
        shiftState: ShiftState,
        language: KeyboardLanguage,
    ) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        suggestionBarRenderer.draw(canvas, keyboard.suggestionBar, suggestions)
        keyboard.keys.forEach { key ->
            keyRenderer.draw(canvas, key, theme, pressedKeys.isPressed(key.spec.id), shiftState, language)
        }
    }
}
