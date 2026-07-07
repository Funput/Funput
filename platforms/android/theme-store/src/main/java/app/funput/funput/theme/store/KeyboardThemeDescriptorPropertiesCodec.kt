package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

internal object KeyboardThemeDescriptorPropertiesCodec {
    private const val FormatVersion = "1"

    fun encode(descriptor: KeyboardThemeDescriptor, output: OutputStream) {
        val properties = Properties().apply {
            setProperty("formatVersion", FormatVersion)
            setProperty("id", descriptor.id.value)
            setProperty("version", descriptor.version.toString())
            setProperty("name", descriptor.name)
            setProperty("author", descriptor.author)
            setProperty("origin", descriptor.origin.name)
            descriptor.backgroundImage?.let { background ->
                setProperty("background.source", background.source)
                setProperty("background.opacity", background.opacity.toString())
            }
            putTheme(descriptor.theme)
        }
        properties.store(output, "Funput custom keyboard theme")
    }

    fun decode(input: InputStream): KeyboardThemeDescriptor {
        val properties = Properties().apply { load(input) }
        require(properties.string("formatVersion") == FormatVersion) {
            "Unsupported custom theme format version"
        }
        return KeyboardThemeDescriptor(
            id = KeyboardThemeId.of(properties.string("id")),
            version = properties.int("version"),
            name = properties.string("name"),
            author = properties.string("author"),
            origin = KeyboardThemeOrigin.valueOf(properties.string("origin")),
            backgroundImage = properties.backgroundImageOrNull(),
            theme = properties.theme(),
        )
    }

    private fun Properties.putTheme(theme: KeyboardTheme) {
        setProperty("theme.backgroundStartColor", theme.backgroundStartColor.toString())
        setProperty("theme.backgroundEndColor", theme.backgroundEndColor.toString())
        setProperty("theme.keyColor", theme.keyColor.toString())
        setProperty("theme.specialKeyColor", theme.specialKeyColor.toString())
        setProperty("theme.keyBorderColor", theme.keyBorderColor.toString())
        setProperty("theme.keyShadowColor", theme.keyShadowColor.toString())
        setProperty("theme.pressedKeyColor", theme.pressedKeyColor.toString())
        setProperty("theme.pressedKeyBorderColor", theme.pressedKeyBorderColor.toString())
        setProperty("theme.activatedKeyColor", theme.activatedKeyColor.toString())
        setProperty("theme.activatedKeyBorderColor", theme.activatedKeyBorderColor.toString())
        setProperty("theme.labelColor", theme.labelColor.toString())
        setProperty("theme.secondaryLabelColor", theme.secondaryLabelColor.toString())
        setProperty("theme.accentColor", theme.accentColor.toString())
        setProperty("theme.keyCornerRadiusDp", theme.keyCornerRadiusDp.toString())
        setProperty("theme.keyBorderWidthDp", theme.keyBorderWidthDp.toString())
        setProperty("theme.keyShadowOffsetDp", theme.keyShadowOffsetDp.toString())
        setProperty("theme.pressedKeyShadowOffsetDp", theme.pressedKeyShadowOffsetDp.toString())
    }

    private fun Properties.theme(): KeyboardTheme = KeyboardTheme(
        backgroundStartColor = int("theme.backgroundStartColor"),
        backgroundEndColor = int("theme.backgroundEndColor"),
        keyColor = int("theme.keyColor"),
        specialKeyColor = int("theme.specialKeyColor"),
        keyBorderColor = int("theme.keyBorderColor"),
        keyShadowColor = int("theme.keyShadowColor"),
        pressedKeyColor = int("theme.pressedKeyColor"),
        pressedKeyBorderColor = int("theme.pressedKeyBorderColor"),
        activatedKeyColor = int("theme.activatedKeyColor"),
        activatedKeyBorderColor = int("theme.activatedKeyBorderColor"),
        labelColor = int("theme.labelColor"),
        secondaryLabelColor = int("theme.secondaryLabelColor"),
        accentColor = int("theme.accentColor"),
        keyCornerRadiusDp = float("theme.keyCornerRadiusDp"),
        keyBorderWidthDp = float("theme.keyBorderWidthDp"),
        keyShadowOffsetDp = float("theme.keyShadowOffsetDp"),
        pressedKeyShadowOffsetDp = float("theme.pressedKeyShadowOffsetDp"),
    )

    private fun Properties.backgroundImageOrNull(): KeyboardThemeBackgroundImage? {
        val source = getProperty("background.source") ?: return null
        return KeyboardThemeBackgroundImage(
            source = source,
            opacity = float("background.opacity"),
        )
    }

    private fun Properties.string(key: String): String =
        requireNotNull(getProperty(key)) { "Missing custom theme property: $key" }

    private fun Properties.int(key: String): Int = string(key).toInt()

    private fun Properties.float(key: String): Float = string(key).toFloat()
}
