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
                    KeySoundVolumeSection(model: model, selection: selection)
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
}
