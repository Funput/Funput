package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardLayout

internal fun KeyboardLayout.resolveGeometry(width: Int, height: Int, density: Float): ResolvedKeyboard? {
    if (width <= 0 || height <= 0) return null
    return KeyboardGeometry.resolve(
        layout = this,
        width = width.toFloat(),
        height = height.toFloat(),
        spec = KeyboardGeometrySpec.fromDensity(density),
    )
}
