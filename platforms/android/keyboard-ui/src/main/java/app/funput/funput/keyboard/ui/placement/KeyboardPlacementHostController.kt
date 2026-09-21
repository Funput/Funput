package app.funput.funput.keyboard.ui.placement

import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.KeyboardPlacementResolver
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.keyboard.ui.FunputKeyboardCallbacks
import app.funput.funput.keyboard.ui.KeyboardSafeAreaController
import app.funput.funput.keyboard.ui.placement.onehanded.OneHandedPlacementEditorView
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Measures keyboard content and owns the placement picker and editors. */
internal class KeyboardPlacementHostController(
    private val host: FrameLayout,
    private val contentHost: FrameLayout,
    private val safeArea: KeyboardSafeAreaController,
    private val callbacks: FunputKeyboardCallbacks,
) {
    private val density = host.resources.displayMetrics.density
    private val elevatedEditor = KeyboardPlacementEditorView(host.context)
    private val oneHandedEditor = OneHandedPlacementEditorView(host.context)
    private val picker = KeyboardPlacementModePickerView(host.context)

    var preferences = KeyboardPlacementPreferences.Default
        set(value) {
            if (field == value) return
            field = value
            host.requestLayout()
        }

    init {
        elevatedEditor.onOffsetPreview = ::previewOffset
        elevatedEditor.onOffsetSettled = ::settleOffset
        elevatedEditor.onDone = ::hideEditors
        oneHandedEditor.onWidthPreview = ::previewWidth
        oneHandedEditor.onWidthSettled = { settle(preferences.copy(oneHandedWidthFraction = it)) }
        oneHandedEditor.onSideToggle = ::toggleSide
        oneHandedEditor.onDone = ::hideEditors
        picker.onModeSelected = ::selectMode
        picker.onCancel = ::hidePicker
    }

    fun resolveContentWidth(viewportWidthPx: Int): Int {
        val result = KeyboardPlacementResolver.resolveHorizontal(preferences, viewportWidthPx)
        val params = contentHost.layoutParams as FrameLayout.LayoutParams
        params.width = result.contentWidthPx
        params.height = FrameLayout.LayoutParams.MATCH_PARENT
        params.gravity = if (preferences.oneHandedSide == OneHandedSide.RIGHT) Gravity.RIGHT else Gravity.LEFT
        contentHost.layoutParams = params
        oneHandedEditor.render(preferences.oneHandedWidthFraction, preferences.oneHandedSide)
        return result.contentWidthPx
    }

    fun resolveHeight(baseHeightPx: Int, heightMeasureSpec: Int): Int {
        val viewport = availableContentViewportPx().coerceAtLeast(baseHeightPx)
        val result = KeyboardPlacementResolver.resolve(preferences, density, viewport, baseHeightPx)
        safeArea.extraBottomInset = result.appliedOffsetPx
        elevatedEditor.render(result.maximumOffsetPx, result.appliedOffsetPx)
        val desired = baseHeightPx + safeArea.bottomInset + result.appliedOffsetPx
        return View.resolveSize(desired, heightMeasureSpec)
    }

    fun showPicker() {
        hideEditors()
        attach(picker)
        picker.render(preferences.activeMode)
    }

    fun updateTheme(theme: KeyboardTheme) {
        picker.updateTheme(theme)
        elevatedEditor.updateTheme(theme)
        oneHandedEditor.updateTheme(theme)
    }

    private fun selectMode(mode: KeyboardPlacementMode) {
        settle(preferences.copy(activeMode = mode))
        hidePicker()
        when (mode) {
            KeyboardPlacementMode.STANDARD -> Unit
            KeyboardPlacementMode.ELEVATED -> attach(elevatedEditor)
            KeyboardPlacementMode.ONE_HANDED -> attach(oneHandedEditor)
        }
    }

    private fun previewOffset(offsetDp: Float) {
        preferences = if (offsetDp <= 0f) preferences.copy(activeMode = KeyboardPlacementMode.STANDARD)
        else preferences.copy(activeMode = KeyboardPlacementMode.ELEVATED, elevatedOffsetDp = offsetDp)
    }

    private fun settleOffset(offsetDp: Float) {
        val value = if (offsetDp <= 0f) preferences.copy(activeMode = KeyboardPlacementMode.STANDARD)
        else preferences.copy(activeMode = KeyboardPlacementMode.ELEVATED, elevatedOffsetDp = offsetDp)
        settle(value)
    }

    private fun previewWidth(width: Float) {
        preferences = preferences.copy(
            activeMode = KeyboardPlacementMode.ONE_HANDED,
            oneHandedWidthFraction = width,
        )
    }

    private fun toggleSide() {
        val side = if (preferences.oneHandedSide == OneHandedSide.RIGHT) OneHandedSide.LEFT
        else OneHandedSide.RIGHT
        settle(preferences.copy(activeMode = KeyboardPlacementMode.ONE_HANDED, oneHandedSide = side))
    }

    private fun settle(value: KeyboardPlacementPreferences) {
        preferences = value
        callbacks.dispatchPlacement(value)
    }

    private fun attach(view: View) {
        if (view.parent == null) host.addView(view, matchParent())
        view.visibility = View.VISIBLE
        view.bringToFront()
    }

    private fun hidePicker() { picker.visibility = View.GONE }
    private fun hideEditors() {
        elevatedEditor.visibility = View.GONE
        oneHandedEditor.visibility = View.GONE
    }

    private fun availableContentViewportPx(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return (host.resources.configuration.screenHeightDp * density).roundToInt()
        }
        val height = host.context.getSystemService(WindowManager::class.java)
            .currentWindowMetrics.bounds.height()
        val insets = ViewCompat.getRootWindowInsets(host)
        val top = insets?.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
        )?.top ?: 0
        return (height - top - safeArea.bottomInset).coerceAtLeast(0)
    }

    private fun matchParent() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
    )
}
