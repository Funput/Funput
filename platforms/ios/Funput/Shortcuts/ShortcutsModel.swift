#if DEBUG
import Foundation
import Observation

struct ShortcutDraft: Identifiable, Equatable {
    let id: UUID
    var trigger: String
    var expansion: String

    init(id: UUID = UUID(), trigger: String = "", expansion: String = "") {
        self.id = id
        self.trigger = trigger
        self.expansion = expansion
    }

    var isValid: Bool {
        !trigger.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !expansion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

/// Design-review state only. Never writes to shared keyboard configuration.
@MainActor @Observable
final class ShortcutsModel {
    private(set) var entries: [ShortcutDraft]
    var isEnabled = true
    var smartCase = true
    var inEnglish = true
    var query = ""

    init(entries: [ShortcutDraft] = ShortcutsModel.samples) {
        self.entries = entries
    }

    var filteredEntries: [ShortcutDraft] {
        let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !search.isEmpty else { return entries }
        return entries.filter {
            $0.trigger.localizedCaseInsensitiveContains(search)
                || $0.expansion.localizedCaseInsensitiveContains(search)
        }
    }

    func contains(_ draft: ShortcutDraft) -> Bool {
        entries.contains { $0.id == draft.id }
    }

    func isDuplicate(_ draft: ShortcutDraft) -> Bool {
        !draft.trigger.isEmpty && entries.contains {
            $0.id != draft.id && $0.trigger == draft.trigger
        }
    }

    func save(_ draft: ShortcutDraft) {
        guard draft.isValid else { return }
        if let index = entries.firstIndex(where: { $0.id == draft.id }) {
            entries[index] = draft
        } else {
            entries.append(draft)
            query = ""
        }
    }

    func delete(_ draft: ShortcutDraft) {
        entries.removeAll { $0.id == draft.id }
    }

    static let samples = [
        ShortcutDraft(trigger: "vn", expansion: "việt nam"),
        ShortcutDraft(trigger: "kg", expansion: "không"),
        ShortcutDraft(trigger: "dc", expansion: "được"),
        ShortcutDraft(
            trigger: "camon",
            expansion: "Cảm ơn bạn đã liên hệ.\nMình đã nhận được thông tin và sẽ phản hồi sớm nhất có thể."
        ),
    ]
}
#endif
