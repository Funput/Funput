import FunputShared

@MainActor
enum ShortcutsStoreFactory {
    static var isUITesting: Bool {
#if DEBUG
        ShortcutsDebugStoreFactory.isUITesting
#else
        false
#endif
    }

    static func make() -> any ShortcutsStoring {
#if DEBUG
        ShortcutsDebugStoreFactory.make()
#else
        ShortcutsStore.shared
#endif
    }
}
