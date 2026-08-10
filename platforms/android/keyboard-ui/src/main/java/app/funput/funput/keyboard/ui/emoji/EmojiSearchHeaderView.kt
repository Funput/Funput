package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

internal class EmojiSearchHeaderView(context: Context) : FrameLayout(context) {
    var onSearchRequested: () -> Unit = {}
    var onClearRequested: () -> Unit = {}
    var onCancelRequested: () -> Unit = {}
    private val field = TextView(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(34), 0, dp(34), 0)
        textSize = 15f
        isClickable = true
        isFocusable = true
        setOnClickListener { onSearchRequested() }
    }
    private val search = TextView(context).apply {
        text = "⌕"
        gravity = Gravity.CENTER
        textSize = 22f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val clear = TextView(context).apply {
        configureAction("×")
        contentDescription = "Xóa tìm kiếm"
        setOnClickListener { onClearRequested() }
    }
    private val cancel = TextView(context).apply {
        configureAction("Hủy")
        setOnClickListener { onCancelRequested() }
    }

    init {
        addView(field)
        addView(search, LayoutParams(dp(34), dp(38)))
        addView(clear, LayoutParams(dp(38), dp(38), Gravity.END))
        addView(cancel, LayoutParams(dp(54), dp(46), Gravity.END))
    }

    fun render(state: EmojiSearchState, palette: KeyboardPanelPalette) {
        val browsing = state.mode == EmojiSearchMode.BROWSING
        cancel.visibility = if (browsing) GONE else VISIBLE
        clear.visibility = if (!browsing && state.query.isNotEmpty()) VISIBLE else GONE
        field.text = state.query.ifEmpty { "Tìm kiếm biểu tượng" }
        field.contentDescription = "Tìm kiếm biểu tượng"
        field.setTextColor(if (state.query.isEmpty()) palette.secondaryLabel else palette.label)
        field.background = surface(palette.searchSurface, 9, this)
        search.setTextColor(palette.secondaryLabel)
        clear.setTextColor(palette.secondaryLabel)
        cancel.setTextColor(palette.accent)
        clear.layoutParams = LayoutParams(dp(38), dp(38)).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            marginEnd = dp(60)
        }
        val right = if (browsing) dp(8) else dp(60)
        field.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(38)).apply {
            gravity = Gravity.CENTER_VERTICAL
            marginStart = dp(8)
            marginEnd = right
        }
        search.layoutParams = LayoutParams(dp(34), dp(38)).apply {
            gravity = Gravity.CENTER_VERTICAL
            marginStart = dp(8)
        }
    }
}
