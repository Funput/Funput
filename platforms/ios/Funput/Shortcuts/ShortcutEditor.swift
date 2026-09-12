#if DEBUG
import SwiftUI
import UIKit

struct ShortcutEditor: View {
    @Environment(\.dismiss) private var dismiss
    let model: ShortcutsModel
    let original: ShortcutDraft
    @State private var draft: ShortcutDraft
    @State private var confirmsDiscard = false
    @State private var confirmsDelete = false
    @FocusState private var focusesTrigger: Bool

    init(model: ShortcutsModel, original: ShortcutDraft) {
        self.model = model
        self.original = original
        _draft = State(initialValue: original)
    }

    private var isEditing: Bool { model.contains(original) }
    private var hasChanges: Bool { draft != original }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Ví dụ: vn", text: $draft.trigger)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .focused($focusesTrigger)
                        .accessibilityLabel("Chữ tắt")
                        .accessibilityIdentifier("shortcuts.editor.trigger")
                } header: { Text("Chữ tắt") } footer: {
                    if model.isDuplicate(draft) {
                        Label("Chữ tắt này đã có trong danh sách.", systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.orange)
                    }
                }
                Section("Nội dung thay thế") {
                    TextField("Ví dụ: việt nam", text: $draft.expansion, axis: .vertical)
                        .lineLimit(5...12)
                        .accessibilityLabel("Nội dung thay thế")
                        .accessibilityIdentifier("shortcuts.editor.expansion")
                }
                Section {
                    Label("Chỉ lưu trong bản xem trước", systemImage: "eye")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                if isEditing {
                    Section {
                        Button("Xoá gõ tắt", role: .destructive) { confirmsDelete = true }
                            .frame(minHeight: 44)
                            .accessibilityIdentifier("shortcuts.editor.delete")
                    }
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(isEditing ? "Sửa gõ tắt" : "Thêm gõ tắt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ") {
                        if hasChanges { confirmsDiscard = true } else { dismiss() }
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") { model.save(draft); dismiss() }
                        .disabled(!draft.isValid)
                        .accessibilityIdentifier("shortcuts.editor.save")
                }
            }
            .background(ShortcutDismissGuard(hasChanges: hasChanges) { confirmsDiscard = true })
            .alert("Bỏ thay đổi?", isPresented: $confirmsDiscard) {
                Button("Tiếp tục sửa", role: .cancel) {}
                Button("Bỏ thay đổi", role: .destructive) { dismiss() }
            } message: { Text("Những thay đổi chưa lưu sẽ bị bỏ.") }
            .alert("Xoá gõ tắt?", isPresented: $confirmsDelete) {
                Button("Huỷ", role: .cancel) {}
                Button("Xoá", role: .destructive) { model.delete(original); dismiss() }
            } message: { Text("Bạn muốn xoá “\(original.trigger)” khỏi danh sách?") }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .task { if !isEditing { focusesTrigger = true } }
    }
}

/// Observes an attempted sheet swipe as well as preventing unsaved dismissal.
private struct ShortcutDismissGuard: UIViewControllerRepresentable {
    let hasChanges: Bool
    let onAttempt: () -> Void

    func makeUIViewController(context: Context) -> Controller { Controller() }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.hasChanges = hasChanges
        controller.onAttempt = onAttempt
        controller.attach()
    }

    final class Controller: UIViewController, UIAdaptivePresentationControllerDelegate {
        var hasChanges = false
        var onAttempt: (() -> Void)?

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            attach()
        }

        func attach() {
            var ancestor: UIViewController? = parent
            while let current = ancestor {
                if let presentation = current.presentationController {
                    presentation.delegate = self
                }
                ancestor = current.parent
            }
        }

        func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
            !hasChanges
        }

        func presentationControllerDidAttemptToDismiss(_ presentationController: UIPresentationController) {
            onAttempt?()
        }
    }
}

#Preview("Form gõ tắt") {
    let model = ShortcutsModel()
    ShortcutEditor(model: model, original: model.entries[0])
}
#endif
