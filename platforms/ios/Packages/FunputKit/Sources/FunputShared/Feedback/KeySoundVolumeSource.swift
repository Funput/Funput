/// Which of the system's volumes Funput's bundled key sounds follow.
///
/// iOS gives apps no way to pick a volume stream, only a way to play: the audio
/// session follows the media volume, while a registered system sound follows the
/// ringer and alerts volume. The system input click always follows the latter, so
/// this does not apply to it.
public enum KeySoundVolumeSource: String, Codable, CaseIterable, Hashable, Sendable {
    case media
    case ringer
}
