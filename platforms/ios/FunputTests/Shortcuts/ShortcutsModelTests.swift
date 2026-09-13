import FunputShared
import Testing
@testable import Funput

@MainActor
struct ShortcutsModelTests {
    @Test("Draft edits remain isolated across reload until explicitly saved")
    func draftIsolationAndStableIdentity() async {
        let model = ShortcutsModel(store: ShortcutsPreviewStore())
        await model.reload()
        let original = model.entries[0]
        var draft = original
        draft.expansion = "Nội dung mới"
        await model.reload()
        #expect(model.entries[0] == original)
        #expect(draft.expansion == "Nội dung mới")
        #expect(await model.save(draft))
        #expect(model.entries[0] == draft)
        #expect(model.entries[0].id == original.id)
        #expect(model.entries.count == 4)
    }

    @Test("Adding clears a search; filtered edits and deletion use stable IDs")
    func filteredMutations() async {
        let model = ShortcutsModel(store: ShortcutsPreviewStore())
        await model.reload()
        model.query = "KHÔNG"
        #expect(model.filteredEntries.map(\.trigger) == ["kg"])
        var draft = model.filteredEntries[0]
        draft.expansion = "không có"
        #expect(await model.save(draft))
        #expect(model.query == "KHÔNG")
        #expect(await model.delete(draft))
        #expect(model.entries.map(\.trigger) == ["vn", "dc", "camon"])
        let new = TextShortcut(trigger: "abc", expansion: "Nội dung")
        #expect(await model.save(new))
        #expect(model.query.isEmpty)
        #expect(model.entries.last == new)
    }

    @Test("Whitespace-only drafts and exact duplicates cannot be saved")
    func validation() async {
        let model = ShortcutsModel(store: ShortcutsPreviewStore(library: ShortcutLibrary()))
        await model.reload()
        #expect(!(await model.save(TextShortcut(trigger: " \n", expansion: "hello"))))
        #expect(!(await model.save(TextShortcut(trigger: "abc", expansion: "\n\t"))))
        let multiline = TextShortcut(trigger: "vn", expansion: " Một\nHai ")
        #expect(await model.save(multiline))
        #expect(!model.isDuplicate(multiline))
        #expect(!(await model.save(TextShortcut(trigger: "vn", expansion: "trùng"))))
        #expect(model.saveError != nil)
        #expect(model.entries == [multiline])
        #expect(await model.save(TextShortcut(trigger: "VN", expansion: "KHÁC")))
    }

    @Test("New model instances retain saved entries and all options")
    func reloadAcrossModels() async {
        let store = ShortcutsPreviewStore()
        let model = ShortcutsModel(store: store)
        await model.reload()
        await model.update(\.isEnabled, to: false)
        await model.update(\.smartCase, to: false)
        await model.update(\.inEnglish, to: false)
        model.query = "VN"
        #expect(model.filteredEntries.map(\.trigger) == ["vn"])
        #expect(await model.delete(model.entries[0]))
        let next = ShortcutsModel(store: store)
        await next.reload()
        #expect(next.entries.count == 3)
        #expect(!next.library.isEnabled && !next.library.smartCase && !next.library.inEnglish)
    }
}
