package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeGradientDirection
import app.funput.funput.theme.MetricClamp
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
 * Every number is coerced through [MetricClamp] on the way in. The theme constructor throws on
 * out-of-range values, so a hand-edited or downloaded file has to be brought into range here —
 * once it is constructed it is too late.
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
        put("keyOpacity", theme.keyOpacity.toDouble())
        put("specialKeyOpacity", theme.specialKeyOpacity.toDouble())
        put("keycapInsetDp", theme.keycapInsetDp.toDouble())
        put("backgroundGradientDirection", theme.backgroundGradientDirection.name)
        put("suggestionDividerColor", theme.suggestionDividerColor)
        put("popupShadowColor", theme.popupShadowColor)
    }

    fun decode(json: JSONObject): KeyboardTheme {
        val keyColor = json.getInt("keyColor")
        val specialKeyColor = json.getInt("specialKeyColor")
        val labelColor = json.getInt("labelColor")
        val secondaryLabelColor = json.getInt("secondaryLabelColor")
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
            secondaryLabelColor = secondaryLabelColor,
            accentColor = accentColor,
            keyCornerRadiusDp = json.metric("keyCornerRadiusDp", MetricClamp.CornerRadiusDp),
            keyBorderWidthDp = json.metric("keyBorderWidthDp", MetricClamp.BorderWidthDp),
            keyShadowOffsetDp = json.metric("keyShadowOffsetDp", MetricClamp.ShadowOffsetDp),
            pressedKeyShadowOffsetDp =
                json.metric("pressedKeyShadowOffsetDp", MetricClamp.ShadowOffsetDp),
            specialLabelColor = json.optInt("specialLabelColor", labelColor),
            accentKeyColor = json.optInt("accentKeyColor", specialKeyColor),
            accentLabelColor = json.optInt("accentLabelColor", accentColor),
            popupSurfaceColor = json.optInt("popupSurfaceColor", keyColor),
            suggestionHighlightColor = json.optInt("suggestionHighlightColor", labelColor),
            pressedKeyScale = json.optMetric("pressedKeyScale", 1f, MetricClamp.PressedKeyScale),
            keyOpacity = json.optMetric("keyOpacity", 1f, MetricClamp.Opacity),
            specialKeyOpacity = json.optMetric("specialKeyOpacity", 1f, MetricClamp.Opacity),
            keycapInsetDp = json.optMetric("keycapInsetDp", 0f, MetricClamp.KeycapInsetDp),
            backgroundGradientDirection = KeyboardThemeGradientDirection.parseOrDefault(
                json.optString("backgroundGradientDirection").takeIf(String::isNotEmpty),
            ),
            suggestionDividerColor = json.optInt("suggestionDividerColor", secondaryLabelColor),
            popupShadowColor = json.optInt("popupShadowColor", DefaultPopupShadowColor),
        )
    }

    private fun JSONObject.metric(key: String, range: ClosedFloatingPointRange<Float>): Float =
        getDouble(key).toFloat().coerceIn(range)

    private fun JSONObject.optMetric(
        key: String,
        fallback: Float,
        range: ClosedFloatingPointRange<Float>,
    ): Float = optDouble(key, fallback.toDouble()).toFloat().coerceIn(range)

    private const val DefaultPopupShadowColor = 0x40000000
}
