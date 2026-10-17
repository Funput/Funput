/// The remembered strength of typing feedback, independent of its on/off switch.
public enum HapticFeedbackStrength: String, Codable, CaseIterable, Hashable, Sendable {
    case light
    case medium
    case strong
}
