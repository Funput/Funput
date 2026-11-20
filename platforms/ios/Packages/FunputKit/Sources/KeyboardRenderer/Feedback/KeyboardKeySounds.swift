#if canImport(UIKit)
import FunputShared
import UIKit

/// Lets a test observe key sounds without an audio session.
@MainActor
protocol KeyboardSoundPlaying {
    /// Loads a bundled style ahead of the first press. Cheap once the style is loaded.
    func prepare(style: KeySoundStyle, volume: KeySoundVolume, source: KeySoundVolumeSource)
    func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume, source: KeySoundVolumeSource)
    /// Lets go of what was loaded. The next `prepare` or `play` loads it again.
    func release()
}

/// The same key sounds in the keyboard and the containing app's settings.
@MainActor
public final class KeyboardKeySounds: KeyboardSoundPlaying {
    private struct Prepared: Equatable {
        let style: KeySoundStyle
        let volume: KeySoundVolume
        let source: KeySoundVolumeSource
    }

    private lazy var pool = KeySoundPlayerPool()
    private lazy var systemSounds = KeySystemSoundBank()
    /// Prepare runs on every touch, so only a new choice reaches the audio queue.
    private var prepared: Prepared?

    public init() {}

    public func prepare(style: KeySoundStyle, volume: KeySoundVolume, source: KeySoundVolumeSource) {
        let choice = Prepared(style: style, volume: volume, source: source)
        guard style != .system, choice != prepared else { return }
        prepared = choice
        switch source {
        case .media: pool.load(style)
        case .ringer: systemSounds.load(style, volume: volume)
        }
    }

    /// Players and an active audio session are only worth holding while the keyboard
    /// can be typed on. Registered system sounds stay: they live in the sound server,
    /// not here, and never touch the session.
    public func release() {
        prepared = nil
        pool.unload()
    }

    /// The system style goes through `playInputClick()`, which only sounds while an
    /// input view that adopts `UIInputViewAudioFeedback` is on screen.
    public func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume, source: KeySoundVolumeSource) {
        guard style != .system else {
            UIDevice.current.playInputClick()
            return
        }
        switch source {
        case .media: pool.play(kind, style: style, gain: volume.gain)
        case .ringer: systemSounds.play(kind, style: style, volume: volume)
        }
    }
}

extension KeySoundVolume {
    /// A fraction of the system volume the sound follows; playback cannot exceed it.
    var gain: Float {
        switch self {
        case .soft: 0.35
        case .medium: 0.65
        case .loud: 1
        }
    }
}
#endif
