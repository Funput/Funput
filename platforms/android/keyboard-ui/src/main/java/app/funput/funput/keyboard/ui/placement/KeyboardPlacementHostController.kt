package app.funput.funput.keyboard.ui.placement

import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.KeyboardPlacementResolver
import app.funput.funput.keyboard.ui.FunputKeyboardCallbacks
import app.funput.funput.keyboard.ui.KeyboardSafeAreaController
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Measures the IME bottom region and owns its temporary placement editor. */
internal class KeyboardPlacementHostController(
    private val host: FrameLayout,
    private val safeArea: KeyboardSafeAreaController,
    private val callbacks: FunputKeyboardCallbacks,
) {
    private val density = host.resources.displayMetrics.density
    private val editor = KeyboardPlacementEditorView(host.context)
    private var result = KeyboardPlacementResolver.resolve(
        KeyboardPlacementPreferences.Default, density, 0, 0,
    )

    var preferences = KeyboardPlacementPreferences.Default
        set(value) {
            if (field == value) return
            field = value
            host.requestLayout()
        }

    init {
        editor.onOffsetPreview = ::previewOffset
        editor.onOffsetSettled = ::settleOffset
        editor.onDone = ::hideEditor
    }

    fun resolveHeight(baseHeightPx: Int, heightMeasureSpec: Int): Int {
        val viewport = availableContentViewportPx().coerceAtLeast(baseHeightPx)
        result = KeyboardPlacementResolver.resolve(preferences, density, viewport, baseHeightPx)
        safeArea.extraBottomInset = result.appliedOffsetPx
        editor.render(result.maximumOffsetPx, result.appliedOffsetPx)
        val desired = baseHeightPx + safeArea.bottomInset + result.appliedOffsetPx
        return View.resolveSize(desired, heightMeasureSpec)
    }

    fun showEditor() {
        if (editor.parent == null) {
            host.addView(editor, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }
        editor.visibility = View.VISIBLE
        editor.bringToFront()
        editor.render(result.maximumOffsetPx, result.appliedOffsetPx)
    }

    fun hideEditor() {
        editor.visibility = View.GONE
    }

    fun updateTheme(theme: KeyboardTheme) = editor.updateTheme(theme)

    private fun previewOffset(offsetDp: Float) {
        preferences = if (offsetDp <= 0f) {
            preferences.copy(activeMode = KeyboardPlacementMode.STANDARD)
        } else {
            KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, offsetDp)
        }
    }

    private fun settleOffset(offsetDp: Float) {
        val mode = if (offsetDp <= 0f) KeyboardPlacementMode.STANDARD
        else KeyboardPlacementMode.ELEVATED
        callbacks.dispatchPlacementMode(mode)
        if (mode == KeyboardPlacementMode.ELEVATED) callbacks.dispatchPlacementOffset(offsetDp)
    }

    private fun availableContentViewportPx(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return (host.resources.configuration.screenHeightDp * density).roundToInt()
        }
        val windowManager = host.context.getSystemService(WindowManager::class.java)
        val height = windowManager.currentWindowMetrics.bounds.height()
        val insets = ViewCompat.getRootWindowInsets(host)
        val top = insets?.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
        )?.top ?: 0
        return (height - top - safeArea.bottomInset).coerceAtLeast(0)
    }
}
