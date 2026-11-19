#if canImport(UIKit)
import AudioToolbox
import FunputShared
import os

/// The bundled style in use, registered as system sounds so iOS plays them at the
/// ringer and alerts volume rather than the media volume.
///
/// A system sound takes no volume when it plays, so each level is a copy of the
/// bundled sound with its gain already applied (see ``KeySoundAttenuation``). System
/// sounds never touch the audio session, so the user's music is left alone and the
/// ringer switch silences them, as it does the system keyboard's clicks.
///
/// Registration reads a file and every play crosses to the sound server, so both
/// stay on one serial queue; callers only enqueue.
final class KeySystemSoundBank: @unchecked Sendable {
    private struct Selection: Equatable {
        let style: KeySoundStyle
        let volume: KeySoundVolume
    }

    private static let log = Logger(subsystem: "app.funput.keyboard", category: "KeySound")

    private let queue = DispatchQueue(label: "app.funput.keyboard.system-key-sound", qos: .userInteractive)
    // Confined to `queue`.
    private var loaded: Selection?
    private var sounds: [KeySoundKind: SystemSoundID] = [:]

    deinit {
        sounds.values.forEach { AudioServicesDisposeSystemSoundID($0) }
    }

    func load(_ style: KeySoundStyle, volume: KeySoundVolume) {
        queue.async { self.loadOnQueue(Selection(style: style, volume: volume)) }
    }

    func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume) {
        queue.async {
            self.loadOnQueue(Selection(style: style, volume: volume))
            guard let sound = self.sounds[kind] else { return }
            AudioServicesPlaySystemSound(sound)
        }
    }

    private func loadOnQueue(_ selection: Selection) {
        guard selection != loaded else { return }
        sounds.values.forEach { AudioServicesDisposeSystemSoundID($0) }
        sounds = [:]
        loaded = selection
        for kind in KeySoundKind.allCases {
            guard let source = KeySoundPlayerPool.resourceURL(style: selection.style, kind: kind),
                  let url = KeySoundAttenuation.url(for: source, gain: selection.volume.gain)
            else {
                Self.log.error("System key sound unavailable: \(selection.style.rawValue, privacy: .public)-\(kind.rawValue, privacy: .public)")
                continue
            }
            var sound: SystemSoundID = 0
            let status = AudioServicesCreateSystemSoundID(url as CFURL, &sound)
            guard status == kAudioServicesNoError else {
                Self.log.error("System key sound failed to register: \(status, privacy: .public)")
                continue
            }
            sounds[kind] = sound
        }
    }
}
#endif
