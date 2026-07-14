import KeyboardRenderer
import SwiftUI
import ThemeSchema

struct ThemeGeometryEditor: View {
    @Environment(\.dismiss) private var dismiss
    let model: AppearanceModel
    @State private var draft: ThemeEditorDraft
    @State private var confirmsCancel = false

    init(model: AppearanceModel, request: ThemeEditorRequest) {
        self.model = model
        _draft = State(initialValue: model.makeDraft(for: request))
    }

    var body: some View {
        NavigationStack {
            AppScreen {
                AppearancePreviewHeader(mode: $draft.previewMode)
                KeyboardPreview(
                    presentation: model.presentation(for: draft),
                    interfaceStyle: draft.previewMode.interfaceStyle,
                    isInteractive: true
                )
                .id(draft.previewMode)
                .frame(height: previewHeight)
                .clipShape(.rect(cornerRadius: 22))
                .shadow(color: .black.opacity(0.14), radius: 16, y: 8)
                ThemeEditorNameCard(name: $draft.customTheme.theme.metadata.name)
                ThemeGeometryControls(draft: $draft)
                resetButton
            }
            .navigationTitle(draft.isNew ? "Tạo theme" : "Chỉnh sửa theme")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar }
            .interactiveDismissDisabled(draft.isDirty)
            .confirmationDialog("Bỏ các thay đổi?", isPresented: $confirmsCancel) {
                Button("Bỏ thay đổi", role: .destructive) { dismiss() }
            }
            .alert("Không thể lưu theme", isPresented: model.saveErrorBinding) {
                Button("Đóng", role: .cancel) {}
            }
        }
    }

    private var previewHeight: CGFloat {
        KeyboardMetrics.phonePortraitHeight(for: model.presentation(for: draft).layout)
    }

    @ToolbarContentBuilder private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("Hủy") { draft.isDirty ? (confirmsCancel = true) : dismiss() }
        }
        ToolbarItem(placement: .confirmationAction) {
            Button("Lưu") { if model.save(draft) { dismiss() } }
                .disabled(!draft.canSave)
                .accessibilityIdentifier("themeEditor.save")
        }
    }

    private var resetButton: some View {
        Button("Khôi phục theme gốc", systemImage: "arrow.counterclockwise") {
            draft.customTheme.theme.geometry = draft.baseTheme.geometry
            draft.customTheme.theme.metrics.cornerRadius = draft.baseTheme.metrics.cornerRadius
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity)
        .accessibilityIdentifier("themeEditor.reset")
    }
}
