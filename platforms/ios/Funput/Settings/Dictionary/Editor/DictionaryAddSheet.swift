import PersonalDictionary
import SwiftUI

/// Adds one word by hand: a name, a brand, a nickname the bar should offer before
/// the user has ever typed it.
struct DictionaryAddSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var model: PersonalDictionaryModel
    @State private var draft = DictionaryWordDraft()
    @State private var message: String?
    @State private var isSaving = false
    @State private var confirmsDiscard = false
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Ví dụ: Funput", text: wordBinding)
                        .focused($focused)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .submitLabel(.done)
                        .onSubmit(save)
                        .accessibilityIdentifier("dictionary.add.word")
                } header: {
                    Text("Từ mới")
                } footer: {
                    footer
                }
                if draft.canSave {
                    Section("Thanh gợi ý sẽ hiện") {
                        SuggestionChip(text: draft.suggestionPreview)
                    }
                }
            }
            .navigationTitle("Thêm từ")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Huỷ") {
                        if draft.word.isEmpty { dismiss() } else { confirmsDiscard = true }
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Thêm", action: save)
                        .disabled(!draft.canSave || isSaving)
                        .accessibilityIdentifier("dictionary.add.save")
                }
            }
            .alert("Bỏ từ đang nhập?", isPresented: $confirmsDiscard) {
                Button("Tiếp tục nhập", role: .cancel) {}
                Button("Bỏ", role: .destructive) { dismiss() }
            }
        }
        .presentationDetents([.medium, .large])
        .background(ShortcutDismissGuard(hasChanges: !draft.word.isEmpty || isSaving) {
            confirmsDiscard = !isSaving
        })
        .onAppear { focused = true }
    }

    /// Typing clears the last save's complaint, which was about the word before.
    private var wordBinding: Binding<String> {
        Binding(get: { draft.text }, set: { draft.text = $0; message = nil })
    }

    @ViewBuilder private var footer: some View {
        if let text = draft.problem?.message ?? message {
            Label(text, systemImage: "exclamationmark.circle")
                .foregroundStyle(.red)
                .accessibilityIdentifier("dictionary.add.problem")
        } else {
            Text("Từ tự thêm được gợi ý ngay, kể cả khi bạn chưa gõ nó lần nào. Thêm một từ đang bị chặn sẽ bỏ chặn từ đó.")
        }
    }

    private func save() {
        guard draft.canSave, !isSaving else { return }
        isSaving = true
        Task {
            let outcome = await model.apply(.add, to: draft.word)
            isSaving = false
            if outcome == .applied { return dismiss() }
            message = outcome?.addMessage ?? model.actionError
            model.actionError = nil
        }
    }
}

/// The word as the suggestion bar will draw it: one slot, in the bar's own type size.
private struct SuggestionChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 17))
            .padding(.horizontal, 22)
            .padding(.vertical, 8)
            .background(.thinMaterial, in: Capsule())
            .frame(maxWidth: .infinity)
            .accessibilityLabel("Gợi ý \(text)")
            .accessibilityIdentifier("dictionary.add.preview")
    }
}
