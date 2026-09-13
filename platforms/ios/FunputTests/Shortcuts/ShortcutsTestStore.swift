import FunputShared

actor ShortcutsTestStore: ShortcutsStoring {
    private var library: ShortcutLibrary
    private var rejectsReads = false
    private var rejectsWrites = false
    private var holdWrites = false
    private var pendingWrite: CheckedContinuation<Void, Never>?
    private var writeObserver: CheckedContinuation<Void, Never>?

    init(library: ShortcutLibrary) { self.library = library }
    func rejectReads(_ value: Bool) { rejectsReads = value }
    func rejectWrites(_ value: Bool) { rejectsWrites = value }
    func suspendWrites() { holdWrites = true }

    func load() throws -> ShortcutLibrary {
        if rejectsReads { throw ShortcutsStorageError.readFailed }
        return library
    }

    func save(_ library: ShortcutLibrary) async throws {
        if rejectsReads { throw ShortcutsStorageError.readFailed }
        if holdWrites {
            await withCheckedContinuation { continuation in
                pendingWrite = continuation
                writeObserver?.resume()
                writeObserver = nil
            }
        }
        if rejectsWrites { throw ShortcutsStorageError.writeFailed }
        self.library = library
    }

    func waitForWrite() async {
        if pendingWrite == nil {
            await withCheckedContinuation { writeObserver = $0 }
        }
    }

    func releaseWrite() {
        holdWrites = false
        pendingWrite?.resume()
        pendingWrite = nil
    }
}
