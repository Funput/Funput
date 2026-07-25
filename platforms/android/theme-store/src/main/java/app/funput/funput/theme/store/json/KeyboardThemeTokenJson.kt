package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardTheme
import org.json.JSONObject

/**
 * Maps the [KeyboardTheme] token set to and from JSON.
 *
 * Core tokens are required so a truncated or corrupt file fails loudly instead of silently
 * becoming a half-invented theme. Tokens introduced later are read optionally against the same
 * fallback [KeyboardTheme] itself declares, which is what lets a theme written by an older build
 * keep loading with no migration step, and lets a newer build's file load on an older app because
 * unknown keys are simply ignored.
 *
 * Adding a token: append one `put` here and one optional read below, matching the default in
 * [KeyboardTheme]. Nothing else in the storage layer needs to change.
 */
internal object KeyboardThemeTokenJson {
    fun encode(theme: KeyboardTheme): JSONObject = JSONObject().apply {
        put("backgroundStartColor", theme.backgroundStartColor)
        put("backgroundEndColor", theme.backgroundEndColor)
        put("keyColor", theme.keyColor)
        put("specialKeyColor", theme.specialKeyColor)
        put("keyBorderColor", theme.keyBorderColor)
        put("keyShadowColor", theme.keyShadowColor)
        put("pressedKeyColor", theme.pressedKeyColor)
        put("pressedKeyBorderColor", theme.pressedKeyBorderColor)
        put("activatedKeyColor", theme.activatedKeyColor)
        put("activatedKeyBorderColor", theme.activatedKeyBorderColor)
        put("labelColor", theme.labelColor)
        put("secondaryLabelColor", theme.secondaryLabelColor)
        put("accentColor", theme.accentColor)
        put("keyCornerRadiusDp", theme.keyCornerRadiusDp.toDouble())
        put("keyBorderWidthDp", theme.keyBorderWidthDp.toDouble())
        put("keyShadowOffsetDp", theme.keyShadowOffsetDp.toDouble())
        put("pressedKeyShadowOffsetDp", theme.pressedKeyShadowOffsetDp.toDouble())
        put("specialLabelColor", theme.specialLabelColor)
        put("accentKeyColor", theme.accentKeyColor)
        put("accentLabelColor", theme.accentLabelColor)
        put("popupSurfaceColor", theme.popupSurfaceColor)
        put("suggestionHighlightColor", theme.suggestionHighlightColor)
        put("pressedKeyScale", theme.pressedKeyScale.toDouble())
    }

    fun decode(json: JSONObject): KeyboardTheme {
        val keyColor = json.getInt("keyColor")
        val specialKeyColor = json.getInt("specialKeyColor")
        val labelColor = json.getInt("labelColor")
        val accentColor = json.getInt("accentColor")
        return KeyboardTheme(
            backgroundStartColor = json.getInt("backgroundStartColor"),
            backgroundEndColor = json.getInt("backgroundEndColor"),
            keyColor = keyColor,
            specialKeyColor = specialKeyColor,
            keyBorderColor = json.getInt("keyBorderColor"),
            keyShadowColor = json.getInt("keyShadowColor"),
            pressedKeyColor = json.getInt("pressedKeyColor"),
            pressedKeyBorderColor = json.getInt("pressedKeyBorderColor"),
            activatedKeyColor = json.getInt("activatedKeyColor"),
            activatedKeyBorderColor = json.getInt("activatedKeyBorderColor"),
            labelColor = labelColor,
            secondaryLabelColor = json.getInt("secondaryLabelColor"),
            accentColor = accentColor,
            keyCornerRadiusDp = json.float("keyCornerRadiusDp"),
            keyBorderWidthDp = json.float("keyBorderWidthDp"),
            keyShadowOffsetDp = json.float("keyShadowOffsetDp"),
            pressedKeyShadowOffsetDp = json.float("pressedKeyShadowOffsetDp"),
            specialLabelColor = json.optInt("specialLabelColor", labelColor),
            accentKeyColor = json.optInt("accentKeyColor", specialKeyColor),
            accentLabelColor = json.optInt("accentLabelColor", accentColor),
            popupSurfaceColor = json.optInt("popupSurfaceColor", keyColor),
            suggestionHighlightColor = json.optInt("suggestionHighlightColor", labelColor),
            pressedKeyScale = json.optDouble("pressedKeyScale", 1.0).toFloat(),
        )
    }

    private fun JSONObject.float(key: String): Float = getDouble(key).toFloat()
}
