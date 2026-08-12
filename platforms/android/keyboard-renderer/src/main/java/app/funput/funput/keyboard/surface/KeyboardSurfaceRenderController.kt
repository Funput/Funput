package app.funput.funput.keyboard.surface

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.rendering.KeyboardCanvasRenderer

internal class KeyboardSurfaceRenderController(
    context: Context,
    resources: Resources,
    private val invalidate: () -> Unit,
    private val requestLayout: () -> Unit,
    private val resolveGeometry: () -> Unit,
) {
    private val renderer = KeyboardCanvasRenderer(resources)
    private val background = KeyboardSurfaceBackgroundState(context, invalidate)
    private var width = 0
    private var height = 0
    private val presentation = KeyboardSurfacePresentationState(
        onThemeChanged = { renderer.updateTheme(it, width, height); invalidate() },
        onSizingChanged = { renderer.updateSizing(it); requestLayout(); resolveGeometry(); invalidate() },
        onBackgroundImageChanged = background::update,
        onSuggestionsChanged = invalidate,
        onClipboardHintChanged = invalidate,
        onEnterActionChanged = invalidate,
    )

    var keyboardTheme by presentation::keyboardTheme
    var keyboardThemeBackgroundImage by presentation::keyboardThemeBackgroundImage
    var sizingProfile: KeyboardSizingProfile by presentation::sizingProfile
    var suggestions: List<String> by presentation::suggestions
    var clipboardHint: KeyboardClipboardHint? by presentation::clipboardHint
    var enterAction by presentation::enterAction

    init {
        renderer.updateSizing(sizingProfile)
    }

    fun updateSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        renderer.updateTheme(keyboardTheme, width, height)
    }

    fun draw(
        canvas: Canvas,
        keyboard: ResolvedKeyboard,
        pressedKeys: PressedKeyState,
        shiftState: ShiftState,
        language: KeyboardLanguage,
        editorMode: KeyboardEditorMode,
    ) = renderer.draw(
        canvas,
        width,
        height,
        keyboard,
        background.image,
        background.bitmap,
        suggestions,
        clipboardHint,
        pressedKeys,
        shiftState,
        language,
        enterAction,
        editorMode.isPassword,
    )

    fun clear() {
        background.clear()
    }
}
