/// How big the keys are and how far apart they sit.
///
/// Independent of ``KeyboardLayoutPreset``: a preset decides which keys exist, this
/// decides their metrics, so someone who switched from the stock keyboard can keep
/// Funput's arrangement with Apple's spacing, or the other way round.
public enum KeyboardKeySizing: String, CaseIterable, Hashable, Sendable, Codable {
    /// Funput's own metrics: taller keys, tighter gaps, scaled by the height setting.
    case funput

    /// The row heights and gaps of Apple's stock Vietnamese keyboard, so the keys land
    /// where muscle memory from iOS expects them. Ignores the height setting.
    case system
}
