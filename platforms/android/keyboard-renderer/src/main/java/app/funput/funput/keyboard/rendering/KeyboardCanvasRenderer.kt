package app.funput.funput.keyboard.rendering

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.LocalKeyboardThemeCatalog

/** Draws a fully resolved keyboard without owning Android view state. */
internal class KeyboardCanvasRenderer(resources: Resources) {
    private val metrics = RenderMetrics(resources)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyRenderer = KeyRenderer(metrics)
    private val keyPopupRenderer = KeyPopupRenderer(metrics)
    private val suggestionBarRenderer = SuggestionBarRenderer(metrics)
    private val toolbarLogoRenderer = ToolbarLogoRenderer(resources)
    private val backgroundImageRenderer = KeyboardBackgroundImageRenderer()
    private var theme: KeyboardTheme = LocalKeyboardThemeCatalog.defaultTheme.theme

    fun updateTheme(theme: KeyboardTheme, width: Int, height: Int) {
        this.theme = theme
        keyRenderer.updateTheme(theme)
        suggestionBarRenderer.updateTheme(theme)
        toolbarLogoRenderer.updateTheme(theme)
        if (width > 0 && height > 0) {
            if (theme.backgroundStartColor == theme.backgroundEndColor) {
                backgroundPaint.shader = null
                backgroundPaint.color = theme.backgroundStartColor
            } else {
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
    }

    fun updateSizing(profile: KeyboardSizingProfile) {
        metrics.updateLabelScale(profile.labelScale)
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        keyboard: ResolvedKeyboard,
        backgroundImage: KeyboardThemeBackgroundImage?,
        backgroundBitmap: Bitmap?,
        suggestions: List<String>,
        pressedKeys: PressedKeyState,
        shiftState: ShiftState,
        language: KeyboardLanguage,
        enterAction: KeyboardEnterAction,
        secure: Boolean,
    ) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        backgroundImageRenderer.draw(canvas, backgroundBitmap, backgroundImage, width, height)
        keyboard.suggestionBar?.let { bar ->
            if (bar.suggestionsEnabled) {
                suggestionBarRenderer.draw(canvas, bar, suggestions, pressedKeys)
            }
            toolbarLogoRenderer.draw(canvas, bar.logoBounds)
        }
        keyboard.keys.forEach { key ->
            keyRenderer.draw(
                canvas,
                key,
                theme,
                pressedKeys.isPressed(key.spec.id),
                shiftState,
                language,
                enterAction,
            )
        }
        keyboard.keys.forEach { key ->
            if (pressedKeys.isPressed(key.spec.id) && KeyPopupLayout.isEligible(key, secure)) {
                keyPopupRenderer.draw(canvas, key, width.toFloat(), theme, shiftState)
            }
        }
    }
}
