/// How loud Funput's bundled key sounds play, relative to the system volume they follow
/// (see ``KeySoundVolumeSource``).
///
/// The system input click has no volume of its own, so this does not apply to it.
public enum KeySoundVolume: String, Codable, CaseIterable, Hashable, Sendable {
    case soft
    case medium
    case loud
}
