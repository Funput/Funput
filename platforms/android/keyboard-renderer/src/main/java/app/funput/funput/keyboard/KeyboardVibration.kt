package app.funput.funput.keyboard

/**
 * One key vibration, described before it is played so the choice can be tested without a device.
 *
 * Strengths are scales of one waveform wherever the device allows it. Predefined effects are
 * tuned by each vendor, so their order is not guaranteed: on a Galaxy S21 Ultra the heavy click
 * is longer but softer than the click, and "Mạnh" felt weaker than "Trung bình".
 */
internal sealed interface KeyboardVibration {
    /** A composition primitive (Android 11+) at [scale], from 0 to 1. */
    data class Primitive(val primitiveId: Int, val scale: Float) : KeyboardVibration

    /** A short pulse at [amplitude], from 1 to 255, for vibrators with amplitude control. */
    data class Pulse(val durationMs: Long, val amplitude: Int) : KeyboardVibration

    /** A vendor-tuned predefined effect (Android 10+), the last resort before system feedback. */
    data class Predefined(val effectId: Int) : KeyboardVibration
}

/** What the device's vibrator can play, read once from it. */
internal data class KeyboardVibratorCapabilities(
    val sdkInt: Int,
    val supportedPrimitives: Set<Int> = emptySet(),
    val hasAmplitudeControl: Boolean = false,
)
