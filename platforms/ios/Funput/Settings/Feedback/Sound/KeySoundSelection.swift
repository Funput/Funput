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
            preview(style, configuration: model.configuration)
        }
        return nil
    }

    func select(_ volume: KeySoundVolume, model: SettingsModel) {
        guard model.selectKeySoundVolume(volume), let style = model.selectedKeySoundStyle else { return }
        preview(style, configuration: model.configuration)
    }

    func select(_ source: KeySoundVolumeSource, model: SettingsModel) {
        guard model.selectKeySoundVolumeSource(source), let style = model.selectedKeySoundStyle else { return }
        preview(style, configuration: model.configuration)
    }

    private func preview(_ style: KeySoundStyle, configuration: FunputConfiguration) {
        guard style != .system else {
            AudioServicesPlaySystemSound(Self.systemClick)
            return
        }
        sounds.play(
            .key,
            style: style,
            volume: configuration.keySoundVolume,
            source: configuration.keySoundVolumeSource
        )
    }
}
