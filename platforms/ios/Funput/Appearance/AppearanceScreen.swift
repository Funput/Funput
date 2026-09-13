import Combine
import FunputShared
import KeyboardRenderer
import SwiftUI
import ThemeRuntime
import ThemeSchema

struct AppearanceScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model: AppearanceModel
    @State private var confirmsReset = false
    @State private var confirmsDelete = false
    @State private var editorRequest: ThemeEditorRequest?

    init(
        store: any FunputConfigurationStoring = FunputConfigurationStore(),
        customStore: any CustomThemeStoring = CustomThemeStore(),
        assetStore: any ThemeAssetStoring = ThemeAssetStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = KeyboardBootstrapSynchronizer()
    ) {
        _model = StateObject(wrappedValue: AppearanceModel(
            store: store,
            customStore: customStore,
            assetStore: assetStore,
            bootstrap: bootstrap
        ))
    }

    var body: some View {
        AppScreen {
            AppearancePreviewHeader(mode: model.previewModeBinding)
            KeyboardPreview(
                presentation: model.previewPresentation,
                backgroundImageData: model.imageData(for: model.previewTheme),
                interfaceStyle: model.previewMode.interfaceStyle,
                isInteractive: true
            )
            // New identity per mode rebuilds the surface instead of cross-fading
            // it, so UIKit never interpolates the glass colors out of range.
            .id(model.previewMode)
            .frame(height: previewHeight)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: .black.opacity(0.14), radius: 16, y: 8)
            .accessibilityLabel("Bản xem trước bàn phím \(model.previewTheme.metadata.name)")

            AppearanceThemeActionBar(
                isApplied: model.isPreviewApplied,
                isCustom: model.previewCustomTheme != nil,
                apply: model.applyPreview,
                customize: { editorRequest = model.editorRequest() }
            )

            ThemeGallery(
                model: model,
                onEdit: { editorRequest = .edit(customThemeID: $0) },
                onDelete: { id in
                    model.selectTheme(id)
                    confirmsDelete = true
                }
            )
            KeyboardAppearanceCard(appearance: model.keyboardAppearanceBinding)
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
        .background {
            if #available(iOS 17, *) {
                Color.clear.onChange(of: scenePhase) { _, p in if p == .active { model.reload() } }
            } else {
                Color.clear.onChange(of: scenePhase) { p in if p == .active { model.reload() } }
            }
        }
    }

    private var previewHeight: CGFloat {
        KeyboardMetrics.phonePortraitHeight(for: model.previewPresentation.layout)
    }
}

#Preview("Giao diện · Light") {
    NavigationStack {
        AppearanceScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            assetStore: PreviewThemeAssetStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.light)
}

#Preview("Giao diện · Dark") {
    NavigationStack {
        AppearanceScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            assetStore: PreviewThemeAssetStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.dark)
}
