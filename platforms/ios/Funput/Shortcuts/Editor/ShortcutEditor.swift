import FunputShared
import SwiftUI
import UIKit

struct ShortcutEditor: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var model: ShortcutsModel
    let original: TextShortcut
    @State private var draft: TextShortcut
    @State private var confirmsDiscard = false
    @State private var confirmsDelete = false
    @FocusState private var focusesTrigger: Bool

    init(model: ShortcutsModel, original: TextShortcut) {
        self.model = model
        self.original = original
        _draft = State(initialValue: original)
    }

    private var isEditing: Bool { model.contains(original) }
    private var hasChanges: Bool { draft != original }

    var body: some View {
        NavigationStack {
            Form {
                if model.loadError != nil { Section { ShortcutsLoadStatus(model: model) } }
                Section {
                    TextField("Ví dụ: vn", text: $draft.trigger)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .focused($focusesTrigger)
                        .accessibilityLabel("Chữ tắt")
                        .accessibilityIdentifier("shortcuts.editor.trigger")
                        .disabled(!model.canWrite)
                } header: { Text("Chữ tắt") } footer: {
                    if model.isDuplicate(draft) {
                        Label("Chữ tắt này đã có trong danh sách. Hãy chọn chữ tắt khác.", systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.red)
                    }
                }
                Section("Nội dung thay thế") {
                    TextField("Ví dụ: việt nam", text: $draft.expansion, axis: .vertical)
                        .lineLimit(5...12)
                        .accessibilityLabel("Nội dung thay thế")
                        .accessibilityIdentifier("shortcuts.editor.expansion")
                        .disabled(!model.canWrite)
                }
                Section {
                    Label("Mở lại bàn phím Funput để nhận thay đổi", systemImage: "keyboard")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                if isEditing {
                    Section {
                        Button("Xoá gõ tắt", role: .destructive) { confirmsDelete = true }
                            .frame(minHeight: 44)
                            .accessibilityIdentifier("shortcuts.editor.delete")
                            .disabled(!model.canWrite)
                    }
                }
            }
            .disabled(model.isSaving)
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(isEditing ? "Sửa gõ tắt" : "Thêm gõ tắt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ") {
                        if hasChanges { confirmsDiscard = true } else { dismiss() }
                    }
                    .disabled(model.isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") {
                        Task { if await model.save(draft) { dismiss() } }
                    }
                        .disabled(!model.canWrite || !draft.isValid || model.isDuplicate(draft))
                        .accessibilityIdentifier("shortcuts.editor.save")
                }
            }
            .background(ShortcutDismissGuard(hasChanges: hasChanges || model.isSaving) {
                if !model.isSaving { confirmsDiscard = true }
            })
            .alert("Bỏ thay đổi?", isPresented: $confirmsDiscard) {
                Button("Tiếp tục sửa", role: .cancel) {}
                Button("Bỏ thay đổi", role: .destructive) { dismiss() }
            } message: { Text("Những thay đổi chưa lưu sẽ bị bỏ.") }
            .alert("Xoá gõ tắt?", isPresented: $confirmsDelete) {
                Button("Huỷ", role: .cancel) {}
                Button("Xoá", role: .destructive) {
                    Task { if await model.delete(original) { dismiss() } }
                }
            } message: { Text("Bạn muốn xoá “\(original.trigger)” khỏi danh sách?") }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .shortcutsSaveAlert(model)
        .task { if !isEditing { focusesTrigger = true } }
    }
}

#if DEBUG
#Preview("Form gõ tắt") {
    let model = ShortcutsModel(store: ShortcutsPreviewStore())
    ShortcutEditor(model: model, original: ShortcutsPreviewStore.samples[0])
        .task { await model.reload() }
}
#endif
