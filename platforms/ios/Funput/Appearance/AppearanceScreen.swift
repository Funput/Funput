import FunputShared
import KeyboardRenderer
import SwiftUI
import ThemeRuntime
import ThemeSchema

struct AppearanceScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: AppearanceModel
    @State private var confirmsReset = false
    @State private var confirmsDelete = false
    @State private var editorRequest: ThemeEditorRequest?

    init(
        store: any FunputConfigurationStoring = FunputConfigurationStore(),
        customStore: any CustomThemeStoring = CustomThemeStore()
    ) {
        _model = State(initialValue: AppearanceModel(store: store, customStore: customStore))
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
            AppearanceCustomizeButton(isCustom: model.previewCustomTheme != nil) {
                editorRequest = model.editorRequest()
            }

            ThemeGallery(
                model: model,
                onEdit: { editorRequest = .edit(customThemeID: $0) },
                onDelete: { id in
                    model.selectTheme(id)
                    confirmsDelete = true
                }
            )
            AppearanceResetCard(isDefault: model.appliedThemeID == FunputConfiguration.defaultThemeID) {
                confirmsReset = true
            }
        }
        .navigationTitle("Giao diện")
        .sheet(item: $editorRequest) { request in
            ThemeEditor(model: model, request: request)
        }
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
        .confirmationDialog("Xóa theme custom?", isPresented: $confirmsDelete) {
            Button("Xóa", role: .destructive) { _ = model.deletePreviewCustomTheme() }
        } message: {
            Text("Nếu theme đang được dùng, Funput sẽ chuyển về theme hệ thống gốc.")
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
    NavigationStack {
        AppearanceScreen(store: PreviewConfigurationStore(), customStore: PreviewCustomThemeStore())
    }
        .preferredColorScheme(.light)
}

#Preview("Giao diện · Dark") {
    NavigationStack {
        AppearanceScreen(store: PreviewConfigurationStore(), customStore: PreviewCustomThemeStore())
    }
        .preferredColorScheme(.dark)
}
