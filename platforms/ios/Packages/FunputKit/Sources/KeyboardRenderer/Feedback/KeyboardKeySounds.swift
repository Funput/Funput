#if canImport(UIKit)
import FunputShared
import UIKit

/// Lets a test observe key sounds without an audio session.
@MainActor
protocol KeyboardSoundPlaying {
    /// Loads a bundled style ahead of the first press. Cheap once the style is loaded.
    func prepare(style: KeySoundStyle)
    func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume)
}

/// The same key sounds in the keyboard and the containing app's settings.
@MainActor
public final class KeyboardKeySounds: KeyboardSoundPlaying {
    private lazy var pool = KeySoundPlayerPool()
    /// Prepare runs on every touch, so only a new style reaches the audio queue.
    private var preparedStyle: KeySoundStyle?

    public init() {}

    public func prepare(style: KeySoundStyle) {
        guard style != .system, style != preparedStyle else { return }
        preparedStyle = style
        pool.load(style)
    }

    /// The system style goes through `playInputClick()`, which only sounds while an
    /// input view that adopts `UIInputViewAudioFeedback` is on screen.
    public func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume) {
        guard style != .system else {
            UIDevice.current.playInputClick()
            return
        }
        pool.play(kind, style: style, gain: volume.gain)
    }
}

extension KeySoundVolume {
    /// A fraction of the media volume; the player cannot exceed it.
    var gain: Float {
        switch self {
        case .soft: 0.35
        case .medium: 0.65
        case .loud: 1
        }
    }
}
#endif
