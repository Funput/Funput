/// How loud Funput's bundled key sounds play, relative to the media volume.
///
/// The system input click has no volume of its own, so this does not apply to it.
public enum KeySoundVolume: String, Codable, CaseIterable, Hashable, Sendable {
    case soft
    case medium
    case loud
}
