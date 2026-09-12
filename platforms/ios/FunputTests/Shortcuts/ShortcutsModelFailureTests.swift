import FunputShared
import Testing
@testable import Funput

@MainActor
struct ShortcutsModelFailureTests {
    @Test("Failed writes preserve saved data, query, draft and toggle values")
    func failedMutations() async {
        let entry = TextShortcut(trigger: "vn", expansion: "việt nam")
        let store = ShortcutsTestStore(library: ShortcutLibrary(entries: [entry]))
        let model = ShortcutsModel(store: store)
        await model.reload()
        await store.rejectWrites(true)
        var draft = entry
        draft.expansion = "sửa lại"
        model.query = "vn"
        #expect(!(await model.save(draft)))
        #expect(model.entries == [entry])
        #expect(draft.expansion == "sửa lại")
        #expect(!(await model.save(TextShortcut(trigger: "abc", expansion: "mới"))))
        #expect(model.query == "vn")
        #expect(!(await model.delete(entry)))
        #expect(model.entries == [entry])
        await model.update(\.isEnabled, to: false)
        #expect(model.library.isEnabled)
        #expect(model.saveError != nil)
        await store.rejectWrites(false)
        #expect(await model.save(draft))
        #expect(model.saveError == nil)
        #expect(model.entries == [draft])
    }

    @Test("Read failures keep the last snapshot and block writes until retry succeeds")
    func failedReload() async {
        let store = ShortcutsTestStore(library: ShortcutLibrary(entries: ShortcutsPreviewStore.samples))
        let model = ShortcutsModel(store: store)
        await store.rejectReads(true)
        await model.reload()
        #expect(!model.hasLoaded && !model.canWrite && model.loadError != nil)
        await store.rejectReads(false)
        await model.reload()
        #expect(model.canWrite)
        let previous = model.entries
        await store.rejectReads(true)
        await model.reload()
        #expect(model.entries == previous)
        #expect(!model.canWrite)
        #expect(!(await model.delete(previous[0])))
        await store.rejectReads(false)
        await model.reload()
        #expect(model.canWrite && model.loadError == nil)
    }

    @Test("An in-flight option write blocks overlapping mutations and rolls back on failure")
    func serializedWrites() async {
        let store = ShortcutsTestStore(library: ShortcutLibrary(entries: ShortcutsPreviewStore.samples))
        let model = ShortcutsModel(store: store)
        await model.reload()
        await store.suspendWrites()
        await store.rejectWrites(true)
        let task = Task { await model.update(\.isEnabled, to: false) }
        await store.waitForWrite()
        #expect(model.isSaving && !model.canWrite && !model.library.isEnabled)
        #expect(!(await model.delete(model.entries[0])))
        await model.reload()
        #expect(!model.isLoading)
        await store.releaseWrite()
        await task.value
        #expect(model.library.isEnabled && model.canWrite)
        #expect(model.entries.count == 4)
    }

    @Test("A file that becomes unreadable before saving locks edits until reload")
    func readFailureDuringSave() async {
        let store = ShortcutsTestStore(library: ShortcutLibrary(entries: ShortcutsPreviewStore.samples))
        let model = ShortcutsModel(store: store)
        await model.reload()
        let previous = model.entries
        await store.rejectReads(true)
        #expect(!(await model.delete(previous[0])))
        #expect(model.loadError != nil && !model.canWrite)
        #expect(model.entries == previous)
        await store.rejectReads(false)
        await model.reload()
        #expect(model.canWrite && model.entries == previous)
    }
}
