package app.funput.funput.ui.kit

/**
 * Where FunputUI captures are written when someone records them locally to look at a screen. They
 * are not committed and not compared: CI composes every screen to catch crashes, and the look is
 * checked by hand on a device.
 */
internal const val KIT_SCREENSHOT_ROOT: String = "build/outputs/roborazzi"
