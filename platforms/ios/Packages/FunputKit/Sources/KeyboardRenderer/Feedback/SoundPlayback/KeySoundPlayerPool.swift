#if canImport(UIKit)
import AVFoundation
import FunputShared
import os

/// Preloaded players for the one bundled style in use.
///
/// Players rather than an audio engine: nothing runs between presses, so a keyboard
/// left open costs no power. Several players per sound let fast typing overlap
/// instead of cutting the previous press short.
///
/// Every audio call can block on the audio server, so the session, the players and
/// playback all live on one serial queue; callers only enqueue. Being serial, the
/// queue also loads a style before it plays the press that asked for it.
final class KeySoundPlayerPool: @unchecked Sendable {
    private static let playersPerKind = 3
    private static let log = Logger(subsystem: "app.funput.keyboard", category: "KeySound")

    private let queue = DispatchQueue(label: "app.funput.keyboard.key-sound", qos: .userInteractive)
    // Confined to `queue`.
    private var isSessionAmbient = false
    private var loadedStyle: KeySoundStyle?
    private var players: [KeySoundKind: [AVAudioPlayer]] = [:]
    private var nextIndex: [KeySoundKind: Int] = [:]

    func load(_ style: KeySoundStyle) {
        queue.async { self.loadOnQueue(style) }
    }

    func play(_ kind: KeySoundKind, style: KeySoundStyle, gain: Float) {
        queue.async {
            self.loadOnQueue(style)
            self.playOnQueue(kind, gain: gain)
        }
    }

    static func resourceURL(style: KeySoundStyle, kind: KeySoundKind) -> URL? {
        Bundle.module.url(forResource: "\(style.rawValue)-\(kind.rawValue)", withExtension: "wav")
    }

    /// Players are made only once the session is ambient: one that played under the
    /// default session would stop the user's music. Until then the style stays
    /// unloaded, so the next press tries again.
    private func loadOnQueue(_ style: KeySoundStyle) {
        guard style != loadedStyle, makeSessionAmbient() else { return }
        loadedStyle = style
        nextIndex = [:]
        players = [:]
        for kind in KeySoundKind.allCases {
            players[kind] = Self.makePlayers(style: style, kind: kind)
        }
    }

    private func playOnQueue(_ kind: KeySoundKind, gain: Float) {
        guard let group = players[kind], !group.isEmpty else { return }
        let index = nextIndex[kind, default: 0]
        nextIndex[kind] = (index + 1) % group.count
        let player = group[index]
        player.volume = gain
        player.currentTime = 0
        player.play()
    }

    /// Ambient mixes with whatever is playing and goes quiet with the ringer switch,
    /// as the system keyboard's clicks do. Only the category is required: playback
    /// activates an ambient session on its own if `setActive` fails.
    private func makeSessionAmbient() -> Bool {
        guard !isSessionAmbient else { return true }
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.ambient)
        } catch {
            Self.log.error("Key sound category failed: \(error.localizedDescription, privacy: .public)")
            return false
        }
        do {
            try session.setActive(true)
        } catch {
            Self.log.error("Key sound activation failed: \(error.localizedDescription, privacy: .public)")
        }
        isSessionAmbient = true
        return true
    }

    private static func makePlayers(style: KeySoundStyle, kind: KeySoundKind) -> [AVAudioPlayer] {
        guard let url = resourceURL(style: style, kind: kind) else {
            log.error("Missing key sound \(style.rawValue, privacy: .public)-\(kind.rawValue, privacy: .public)")
            return []
        }
        do {
            let data = try Data(contentsOf: url)
            return try (0..<playersPerKind).map { _ in
                let player = try AVAudioPlayer(data: data, fileTypeHint: AVFileType.wav.rawValue)
                player.prepareToPlay()
                return player
            }
        } catch {
            log.error("Key sound failed to load: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }
}
#endif
