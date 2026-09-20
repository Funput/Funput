import FunputShared
import Foundation
import Testing
import UniformTypeIdentifiers

@Suite("Clipboard snapshot")
struct ClipboardSnapshotTests {
    /// iOS answers a refused read (`PBErrorDomain` code 10) as though the pasteboard
    /// were empty and untouched since boot, so that shape must not be believed.
    @Test("An all-zero reading is treated as indeterminate")
    func refusedRead() {
        let snapshot = ClipboardSnapshot(
            changeCount: 0, hasStrings: false, hasURLs: false, hasPlainText: false
        )
        #expect(snapshot.isIndeterminate)
    }

    @Test("A pasteboard the user emptied is a real answer, not a refusal")
    func genuinelyEmptied() {
        let snapshot = ClipboardSnapshot(
            changeCount: 184, hasStrings: false, hasURLs: false, hasPlainText: false
        )
        #expect(!snapshot.isIndeterminate)
    }

    @Test("Any content at all makes the reading trustworthy")
    func withContent() {
        #expect(
            !ClipboardSnapshot(
                changeCount: 0, hasStrings: true, hasURLs: false, hasPlainText: true
            ).isIndeterminate
        )
        #expect(
            !ClipboardSnapshot(
                changeCount: 0, hasStrings: false, hasURLs: true, hasPlainText: false
            ).isIndeterminate
        )
        #expect(
            !ClipboardSnapshot(
                changeCount: 0, hasStrings: false, hasURLs: false, hasPlainText: true
            ).isIndeterminate
        )
    }

    @Test("Plain-text types exclude URLs, files and images")
    func acceptedTypes() {
        let types = ClipboardPlainText.typeIdentifiers.compactMap(UTType.init)
        #expect(types.count == ClipboardPlainText.typeIdentifiers.count)
        #expect(types.allSatisfy { $0.conforms(to: .plainText) })
        #expect(!ClipboardPlainText.typeIdentifiers.contains(UTType.url.identifier))
        #expect(!ClipboardPlainText.typeIdentifiers.contains(UTType.fileURL.identifier))
        #expect(!ClipboardPlainText.typeIdentifiers.contains(UTType.image.identifier))
    }

    @Test("Provider matching follows the same plain-text boundary")
    func providerMatching() {
        #expect(ClipboardPlainText.canLoad(from: provider(for: .utf8PlainText)))
        #expect(!ClipboardPlainText.canLoad(from: provider(for: .url)))
        #expect(!ClipboardPlainText.canLoad(from: provider(for: .fileURL)))
        #expect(!ClipboardPlainText.canLoad(from: provider(for: .image)))
    }

    private func provider(for type: UTType) -> NSItemProvider {
        let provider = NSItemProvider()
        provider.registerDataRepresentation(
            forTypeIdentifier: type.identifier, visibility: .all,
            loadHandler: { _ in nil }
        )
        return provider
    }
}
