/// The sound a keypress makes, remembered independently of its on/off switch.
///
/// `system` is the iOS input click, which follows the system's own keyboard sound
/// setting and volume. The others are Funput's bundled sounds.
public enum KeySoundStyle: String, Codable, CaseIterable, Hashable, Sendable {
    case system
    case mechanical
    case wood
    case bubble
    case typewriter
    case marimba
}
