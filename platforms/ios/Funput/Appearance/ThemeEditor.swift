import KeyboardRenderer
import SwiftUI
import ThemeSchema

struct ThemeEditor: View {
    @Environment(\.dismiss) private var dismiss
    let model: AppearanceModel
    @State private var draft: ThemeEditorDraft
    @State private var confirmsCancel = false
    @State private var selectedTab = ThemeEditorTab.general

    init(model: AppearanceModel, request: ThemeEditorRequest) {
        self.model = model
        _draft = State(initialValue: model.makeDraft(for: request))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                ThemeEditorBackground()
                GeometryReader { proxy in
                    VStack(spacing: 0) {
                        ThemeEditorPreview(
                            draft: $draft,
                            presentation: model.presentation(for: draft),
                            availableHeight: proxy.size.height
                        )
                        ThemeEditorTabBar(selection: $selectedTab)
                        ThemeEditorPages(selection: $selectedTab, draft: $draft)
                    }
                }
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
}
