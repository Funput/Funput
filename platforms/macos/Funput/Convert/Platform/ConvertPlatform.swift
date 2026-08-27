import AppKit
import UniformTypeIdentifiers

struct ConvertPlatform {
    private let readClipboard: @MainActor () -> String?
    private let writeClipboard: @MainActor (String) -> Bool
    private let chooseFiles: @MainActor () -> [URL]?
    private let writeFile: @MainActor (Data) throws -> Bool

    init() {
        readClipboard = { NSPasteboard.general.string(forType: .string) }
        writeClipboard = { text in
            let board = NSPasteboard.general
            board.clearContents()
            return board.setString(text, forType: .string)
        }
        chooseFiles = { Self.openPanel() }
        writeFile = { try Self.savePanel($0) }
    }

    init(
        pastedText: @escaping @MainActor () -> String?, copy: @escaping @MainActor (String) -> Bool,
        pickFiles: @escaping @MainActor () -> [URL]?, save: @escaping @MainActor (Data) throws -> Bool
    ) {
        readClipboard = pastedText
        writeClipboard = copy
        chooseFiles = pickFiles
        writeFile = save
    }

    func pastedText() -> String? { readClipboard() }
    func copy(_ text: String) -> Bool { writeClipboard(text) }
    func pickFiles() -> [URL]? { chooseFiles() }
    func save(_ data: Data) throws -> Bool { try writeFile(data) }

    private static func openPanel() -> [URL]? {
        let panel = NSOpenPanel()
        panel.title = "Chọn nội dung cần chuyển mã"
        panel.prompt = "Chọn"
        panel.canChooseFiles = true
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = true
        return panel.runModal() == .OK ? panel.urls : nil
    }

    private static func savePanel(_ data: Data) throws -> Bool {
        let panel = NSSavePanel()
        panel.title = "Lưu văn bản đã chuyển mã"
        panel.nameFieldStringValue = "chuyen-ma.txt"
        panel.allowedContentTypes = [.plainText]
        panel.canCreateDirectories = true
        guard panel.runModal() == .OK, let url = panel.url else { return false }
        try data.write(to: url, options: .atomic)
        return true
    }
}
