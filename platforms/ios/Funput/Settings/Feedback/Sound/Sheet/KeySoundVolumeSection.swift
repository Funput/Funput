import FunputShared
import SwiftUI

/// How loud the bundled sounds play and which system volume they follow.
///
/// Sits above the style list so the sheet's medium detent shows it without scrolling.
/// Both choices are remembered while the system style is selected, but neither
/// applies to it: the system click has its own volume.
struct KeySoundVolumeSection: View {
    @ObservedObject var model: SettingsModel
    @ObservedObject var selection: KeySoundSelection

    var body: some View {
        let style = model.selectedKeySoundStyle
        let adjustable = style != nil && style != .system
        VStack(alignment: .leading, spacing: 8) {
            Text("Âm lượng")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(adjustable ? .primary : .secondary)
                .accessibilityHidden(true)
            Picker("Âm lượng", selection: volumeBinding) {
                ForEach(KeySoundVolume.allCases, id: \.self) { volume in
                    Text(volume.settingsTitle).tag(volume)
                }
            }
            .pickerStyle(.segmented)
            .disabled(!adjustable)
            .accessibilityIdentifier("settings.sound.volume")
            Picker("Theo âm lượng", selection: sourceBinding) {
                ForEach(KeySoundVolumeSource.allCases, id: \.self) { source in
                    Text(source.settingsTitle).tag(source)
                }
            }
            .pickerStyle(.segmented)
            .disabled(!adjustable)
            .accessibilityIdentifier("settings.sound.volumeSource")
            Text(footnote(style))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
    }

    private var volumeBinding: Binding<KeySoundVolume> {
        Binding(
            get: { model.configuration.keySoundVolume },
            set: { selection.select($0, model: model) }
        )
    }

    private var sourceBinding: Binding<KeySoundVolumeSource> {
        Binding(
            get: { model.configuration.keySoundVolumeSource },
            set: { selection.select($0, model: model) }
        )
    }

    private func footnote(_ style: KeySoundStyle?) -> String {
        switch style {
        case nil:
            "Chọn một kiểu âm để chỉnh độ to."
        case .system:
            "Âm Mặc định theo âm lượng và cài đặt âm bàn phím của iOS."
        default:
            switch model.configuration.keySoundVolumeSource {
            case .media:
                "Âm của Funput theo âm lượng phương tiện và im lặng khi bật chế độ im lặng."
            case .ringer:
                "Âm của Funput theo âm lượng chuông và cảnh báo trong Cài đặt của iOS và im lặng khi bật chế độ im lặng."
            }
        }
    }
}

extension KeySoundVolumeSource {
    var settingsTitle: String {
        switch self {
        case .media: "Phương tiện"
        case .ringer: "Chuông và cảnh báo"
        }
    }
}
