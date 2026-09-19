#if canImport(UIKit)
import FunputShared
@testable import KeyboardRenderer
import Testing
import UniformTypeIdentifiers

@MainActor
@Suite("Clipboard plain-text control")
struct ClipboardPlainTextControlTests {
    @Test("Paste control accepts plain text but not URL-shaped providers")
    func acceptedTypes() throws {
        let view = KeyboardClipboardChipView()
        let identifiers = try #require(view.pasteConfiguration?.acceptableTypeIdentifiers)
        #expect(identifiers == ClipboardPlainText.typeIdentifiers)
        #expect(!identifiers.contains(UTType.url.identifier))
        #expect(!identifiers.contains(UTType.fileURL.identifier))
        #expect(!identifiers.contains(UTType.image.identifier))
    }
}
#endif
