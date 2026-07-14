/// The surface material a theme requests for keyboard backgrounds and keys.
///
/// `glass` maps to Liquid Glass on capable systems and degrades to
/// `translucent` when transparency is reduced or the OS lacks glass support.
public enum KeyboardMaterial: String, Hashable, Sendable, Codable {
    case solid
    case translucent
    case glass
}
