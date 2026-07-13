import FunputShared
import KeyboardRenderer
import SwiftUI
import ThemeSchema

struct AppearanceScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: AppearanceModel
    @State private var confirmsReset = false

    init(store: any FunputConfigurationStoring = FunputConfigurationStore()) {
        _model = State(initialValue: AppearanceModel(store: store))
    }

    var body: some View {
        AppScreen {
            AppearancePreviewHeader(mode: model.previewModeBinding)
            KeyboardPreview(
                presentation: model.previewPresentation,
                interfaceStyle: model.previewMode.interfaceStyle,
                isInteractive: true
            )
            // New identity per mode rebuilds the surface instead of cross-fading
            // it, so UIKit never interpolates the glass colors out of range.
            .id(model.previewMode)
            .frame(height: previewHeight)
            .clipShape(.rect(cornerRadius: 22))
            .shadow(color: .black.opacity(0.14), radius: 16, y: 8)
            .accessibilityLabel("Bản xem trước bàn phím \(model.previewTheme.metadata.name)")

            AppearanceApplyButton(isApplied: model.isPreviewApplied) {
                model.applyPreview()
            }

            ThemeGallery(model: model)
            AppearanceResetCard(isDefault: model.appliedThemeID == FunputConfiguration.defaultThemeID) {
                confirmsReset = true
            }
        }
        .navigationTitle("Giao diện")
        .alert("Không thể lưu giao diện", isPresented: model.saveErrorBinding) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text("Theme đang dùng được giữ nguyên. Vui lòng thử lại.")
        }
        .confirmationDialog("Khôi phục Funput Glass?", isPresented: $confirmsReset) {
            Button("Khôi phục", role: .destructive) { model.resetTheme() }
        } message: {
            Text("Các cài đặt bộ gõ khác sẽ được giữ nguyên.")
        }
        .onAppear { model.setInitialMode(for: colorScheme) }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.reload() }
        }
    }

    private var previewHeight: CGFloat {
        KeyboardMetrics.phonePortraitHeight(for: model.previewPresentation.layout)
    }
}

#Preview("Giao diện · Light") {
    NavigationStack { AppearanceScreen(store: PreviewConfigurationStore()) }
        .preferredColorScheme(.light)
}

#Preview("Giao diện · Dark") {
    NavigationStack { AppearanceScreen(store: PreviewConfigurationStore()) }
        .preferredColorScheme(.dark)
}
