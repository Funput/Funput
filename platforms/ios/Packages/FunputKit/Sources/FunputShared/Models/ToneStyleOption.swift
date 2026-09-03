/// Tone-mark placement style, mirrored here as a persistable option.
///
/// Kept independent of the Rust engine's `FunputToneStyle` so shared
/// configuration never links `FunputCore`; `KeyboardInput` bridges the two.
public enum ToneStyleOption: String, CaseIterable, Hashable, Sendable, Codable {
    /// Classic placement (e.g. `hòa`).
    case traditional
    /// Modern placement (e.g. `hoà`).
    case modern
}
