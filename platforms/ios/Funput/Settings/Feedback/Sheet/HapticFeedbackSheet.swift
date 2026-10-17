import SwiftUI

/// Quick access to strength selection and the same feedback as the settings page.
struct HapticFeedbackSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @ObservedObject var model: SettingsModel
    @StateObject private var selection = HapticFeedbackSelection()
    @State private var pendingAccess: FullAccessFeature?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HapticFeedbackList(
                        selection: model.selectedHapticStrength,
                        select: { pendingAccess = selection.select($0, model: model) }
                    )
                    Text("Chạm một mức để cảm nhận phản hồi.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.horizontal, 14)
                }
                .background(HapticFeedbackSelectionHost(selection: selection).allowsHitTesting(false))
                .fullAccessPrompt($pendingAccess, model: model)
                .frame(maxWidth: 560)
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 20)
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Rung khi gõ")
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
