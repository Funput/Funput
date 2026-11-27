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
    @State private var showsPreview = false

    init(
        store: any FunputConfigurationStoring = FunputConfigurationStore(),
        customStore: any CustomThemeStoring = CustomThemeStore(),
        assetStore: any ThemeAssetStoring = ThemeAssetStore(),
        snapshotStore: any ThemeSnapshotStoring = ThemeSnapshotFileStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = KeyboardBootstrapSynchronizer()
    ) {
        _model = StateObject(wrappedValue: AppearanceModel(
            store: store,
            customStore: customStore,
            assetStore: assetStore,
            snapshotStore: snapshotStore,
            bootstrap: bootstrap
        ))
    }

    var body: some View {
        AppScreen {
            ThemeGallery(
                model: model,
                snapshots: model.snapshots,
                onPreview: { showsPreview = true },
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
        .appearancePreviewDock(model: model, isPresented: $showsPreview) {
            editorRequest = model.editorRequest()
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
