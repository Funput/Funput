import Testing
@testable import Funput

@MainActor
struct ShortcutsModelTests {
    @Test("A draft cannot change an existing entry until saved")
    func draftIsolationAndStableIdentity() {
        let model = ShortcutsModel()
        let original = model.entries[0]
        var draft = original
        draft.expansion = "Nội dung mới"
        #expect(model.entries[0] == original)
        model.save(draft)
        #expect(model.entries[0] == draft)
        #expect(model.entries[0].id == original.id)
        #expect(model.entries.count == 4)
    }

    @Test("Adding clears a search, while editing and deleting target stable IDs")
    func filteredMutations() {
        let model = ShortcutsModel()
        model.query = "KHÔNG"
        #expect(model.filteredEntries.map(\.trigger) == ["kg"])
        var draft = model.filteredEntries[0]
        draft.expansion = "không có"
        model.save(draft)
        #expect(model.query == "KHÔNG")
        model.delete(draft)
        #expect(model.entries.map(\.trigger) == ["vn", "dc", "camon"])
        let new = ShortcutDraft(trigger: "abc", expansion: "Nội dung")
        model.save(new)
        #expect(model.query.isEmpty)
        #expect(model.entries.last == new)
    }

    @Test("Whitespace-only drafts are rejected without altering entered content")
    func invalidDrafts() {
        let model = ShortcutsModel(entries: [])
        model.save(ShortcutDraft(trigger: " \n", expansion: "hello"))
        model.save(ShortcutDraft(trigger: "abc", expansion: "\n\t"))
        #expect(model.entries.isEmpty)
        let multiline = ShortcutDraft(trigger: "abc", expansion: " Một\nHai ")
        model.save(multiline)
        #expect(model.entries == [multiline])
    }

    @Test("Duplicate warnings ignore the edited ID and only match exact triggers")
    func duplicates() {
        let model = ShortcutsModel()
        #expect(!model.isDuplicate(model.entries[0]))
        #expect(model.isDuplicate(ShortcutDraft(trigger: "vn")))
        #expect(!model.isDuplicate(ShortcutDraft(trigger: "VN")))
    }

    @Test("Disabling expansion preserves management, and another session starts fresh")
    func sessionIsolation() {
        let model = ShortcutsModel()
        model.isEnabled = false
        model.smartCase = false
        model.inEnglish = false
        model.query = "VN"
        #expect(model.filteredEntries.map(\.trigger) == ["vn"])
        for entry in model.entries { model.delete(entry) }
        #expect(model.entries.isEmpty)
        let next = ShortcutsModel()
        #expect(next.entries.count == 4)
        #expect(next.isEnabled && next.smartCase && next.inEnglish)
    }
}
