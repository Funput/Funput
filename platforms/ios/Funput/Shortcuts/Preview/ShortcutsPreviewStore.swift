#if DEBUG
import FunputShared

actor ShortcutsPreviewStore: ShortcutsStoring {
    private var library: ShortcutLibrary

    init(library: ShortcutLibrary = ShortcutLibrary(entries: ShortcutsPreviewStore.samples)) {
        self.library = library
    }

    func load() -> ShortcutLibrary { library }
    func save(_ library: ShortcutLibrary) throws {
        try library.validate()
        self.library = library
    }

    static let samples = [
        TextShortcut(trigger: "vn", expansion: "việt nam"),
        TextShortcut(trigger: "kg", expansion: "không"),
        TextShortcut(trigger: "dc", expansion: "được"),
        TextShortcut(
            trigger: "camon",
            expansion: "Cảm ơn bạn đã liên hệ.\nMình đã nhận được thông tin và sẽ phản hồi sớm nhất có thể."
        ),
    ]
}
#endif
