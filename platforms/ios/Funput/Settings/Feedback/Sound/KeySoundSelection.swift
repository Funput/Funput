import AudioToolbox
import Combine
import FunputShared
import KeyboardRenderer

/// Shares permission, persistence and sample playback for the sound sheet.
@MainActor
final class KeySoundSelection: ObservableObject {
    /// The system keyboard's click. `playInputClick()` is silent outside a keyboard,
    /// so the app previews the default style with the same system sound instead.
    private static let systemClick: SystemSoundID = 1104
    private let sounds = KeyboardKeySounds()

    func select(_ style: KeySoundStyle?, model: SettingsModel) -> FullAccessFeature? {
        if let style, !model.hasFullAccess { return .keySoundStyle(style) }
        if model.selectKeySoundStyle(style), let style {
            preview(style, volume: model.configuration.keySoundVolume)
        }
        return nil
    }

    func select(_ volume: KeySoundVolume, model: SettingsModel) {
        guard model.selectKeySoundVolume(volume), let style = model.selectedKeySoundStyle else { return }
        preview(style, volume: volume)
    }

    private func preview(_ style: KeySoundStyle, volume: KeySoundVolume) {
        guard style != .system else {
            AudioServicesPlaySystemSound(Self.systemClick)
            return
        }
        sounds.play(.key, style: style, volume: volume)
    }
}
