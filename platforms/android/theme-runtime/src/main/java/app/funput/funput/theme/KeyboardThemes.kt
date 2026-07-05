package app.funput.funput.theme

/** Named keyboard theme presets. */
object KeyboardThemes {
    val Dark: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFF000000.toInt(),
        backgroundEndColor = 0xFF000000.toInt(),
        keyColor = 0xFF2C2C2C.toInt(),
        specialKeyColor = 0xFF353535.toInt(),
        keyBorderColor = 0xFF2C2C2C.toInt(),
        keyShadowColor = 0x00000000,
        pressedKeyColor = 0xFF404040.toInt(),
        pressedKeyBorderColor = 0xFF404040.toInt(),
        activatedKeyColor = 0xFF454545.toInt(),
        activatedKeyBorderColor = 0xFF454545.toInt(),
        labelColor = 0xFFE0E0E0.toInt(),
        secondaryLabelColor = 0xFFA8A8A8.toInt(),
        accentColor = 0xFFC8A951.toInt(),
        keyCornerRadiusDp = 8f,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = 0f,
        pressedKeyShadowOffsetDp = 0f,
    )

    /** Legacy glass preset kept for reference while migrating themes. */
    internal val Aurora: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFF12182B.toInt(),
        backgroundEndColor = 0xFF090D18.toInt(),
        keyColor = 0x2EFFFFFF,
        specialKeyColor = 0x4AFFFFFF,
        keyBorderColor = 0x3DFFFFFF,
        keyShadowColor = 0x52000000,
        pressedKeyColor = 0x668DB8FF,
        pressedKeyBorderColor = 0xCCBBD2FF.toInt(),
        activatedKeyColor = 0x478DB8FF,
        activatedKeyBorderColor = 0x998DB8FF.toInt(),
        labelColor = 0xFFF7F9FF.toInt(),
        secondaryLabelColor = 0xB8E2E8F7.toInt(),
        accentColor = 0xFF8DB8FF.toInt(),
        keyCornerRadiusDp = 11f,
        keyBorderWidthDp = 0.75f,
        keyShadowOffsetDp = 2f,
        pressedKeyShadowOffsetDp = 0.5f,
    )
}
