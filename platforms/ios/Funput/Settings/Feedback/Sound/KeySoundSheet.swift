import FunputShared
import SwiftUI

/// Sound style and volume, with each choice played back as it is made.
struct KeySoundSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @ObservedObject var model: SettingsModel
    @StateObject private var selection = KeySoundSelection()
    @State private var pendingAccess: FullAccessFeature?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    volumeSection
                    KeySoundList(
                        selection: model.selectedKeySoundStyle,
                        select: { pendingAccess = selection.select($0, model: model) }
                    )
                }
                .fullAccessPrompt($pendingAccess, model: model)
                .frame(maxWidth: 560)
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 20)
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Âm thanh khi gõ")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Xong") { dismiss() }
                        .tint(Color.accentColor)
                }
            }
            .alert("Không thể lưu cài đặt", isPresented: model.saveErrorBinding) {
                Button("Đóng", role: .cancel) {}
            } message: {
                Text("Funput đã giữ nguyên giá trị trước đó. Vui lòng thử lại.")
            }
        }
        .hapticFeedbackSheetBackground(reduceTransparency: reduceTransparency)
        .presentationDetents(dynamicTypeSize.isAccessibilitySize ? [.large] : [.medium, .large])
        .presentationDragIndicator(.visible)
    }

    /// Above the list so the medium detent shows it without scrolling.
    private var volumeSection: some View {
        let style = model.selectedKeySoundStyle
        let adjustable = style != nil && style != .system
        return VStack(alignment: .leading, spacing: 8) {
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
            Text(volumeFootnote(style))
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

    private func volumeFootnote(_ style: KeySoundStyle?) -> String {
        switch style {
        case nil:
            "Chọn một kiểu âm để chỉnh độ to."
        case .system:
            "Âm Mặc định theo âm lượng và cài đặt âm bàn phím của iOS."
        default:
            "Âm của Funput theo âm lượng phương tiện và im lặng khi bật chế độ im lặng."
        }
    }
}
