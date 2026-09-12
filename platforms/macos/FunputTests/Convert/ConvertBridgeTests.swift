import Foundation
import XCTest
@testable import Funput

final class ConvertBridgeTests: XCTestCase {
    func testTextUsesCoreResultAndExactLegacyBytes() throws {
        let bridge = try XCTUnwrap(ConvertFFISession())
        bridge.setInput("Việt")
        bridge.setSource(0)
        bridge.setTarget(1)

        let state = bridge.state(
            input: "Việt", charsets: ConvertWorker.loadCharsets(),
            transforms: ConvertWorker.loadTransforms()
        )

        XCTAssertEqual(state.mode, .text)
        XCTAssertEqual(state.source, 0)
        XCTAssertEqual(bridge.resultText(), "ViÖt")
        XCTAssertEqual(bridge.saveBytes(), Data([0x56, 0x69, 0xD6, 0x74]))
    }

    func testBatchPagesKeepGlobalStableIDs() throws {
        let directory = try scratchDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        for index in 0..<501 {
            try Data("tệp \(index)".utf8).write(to: directory.appending(path: "\(index).txt"))
        }
        let bridge = try XCTUnwrap(ConvertFFISession())

        XCTAssertTrue(bridge.adopt(paths: [directory.path]))
        let first = bridge.state(input: "", charsets: [], transforms: [])
        XCTAssertEqual(first.rowsTotal, 501)
        XCTAssertEqual(first.files.count, 500)
        XCTAssertEqual(first.files.first?.id, 0)
        XCTAssertEqual(first.files.last?.id, 499)

        bridge.setRowCount(1_000)
        let expanded = bridge.state(input: "", charsets: [], transforms: [])
        XCTAssertEqual(expanded.files.count, 501)
        XCTAssertEqual(expanded.files.last?.id, 500)
    }

    func testUnreadableOnlySelectionDoesNotRestoreOldPaste() throws {
        let directory = try scratchDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        let broken = directory.appending(path: "hong.txt")
        try Data([0xFF, 0xFE, 0x00]).write(to: broken)
        let bridge = try XCTUnwrap(ConvertFFISession())
        bridge.setInput("Nội dung cũ")

        XCTAssertTrue(bridge.adopt(paths: [broken.path]))
        let state = bridge.state(input: "", charsets: [], transforms: [])
        XCTAssertEqual(state.mode, .empty)
        XCTAssertTrue(state.unreadable.contains("hong.txt"))
    }

    func testStoreUsesInjectedClipboardAndDebouncesToLatestText() async throws {
        var copied = ""
        let platform = ConvertPlatform(
            pastedText: { "Văn bản từ clipboard" },
            copy: { copied = $0; return true }, pickFiles: { nil }, save: { _ in false }
        )
        let store = ConvertStore(platform: platform)

        store.send(.paste)
        try await waitUntil { store.state.mode == .text }
        XCTAssertEqual(store.state.inputText, "Văn bản từ clipboard")

        store.send(.setInput("bản cũ"))
        store.send(.setInput("bản mới"))
        try await waitUntil { store.state.inputText == "bản mới" && store.state.outputText == "bản mới" }
        store.send(.copyResult)
        try await waitUntil { copied == "bản mới" }
        XCTAssertEqual(store.state.progress, "Đã chép kết quả")
    }

    /// The menu is core's, not a list Swift keeps its own copy of.
    func testTransformMenuComesFromCore() {
        let menu = ConvertWorker.loadTransforms()

        XCTAssertEqual(menu.count, 5)
        XCTAssertEqual(menu.map(\.id), Array(0..<5))
        XCTAssertEqual(menu[2].name, "Bỏ dấu tiếng Việt")
        XCTAssertTrue(menu.allSatisfy { !$0.name.isEmpty })
    }

    /// Through the real door: the order decides the answer, and undo takes off one.
    func testTransformsStackInOrderAndUndoTakesOffOne() throws {
        let bridge = try XCTUnwrap(ConvertFFISession())
        bridge.setInput("GỬI VỀ TP. HCM")
        bridge.setSource(0)

        bridge.apply(.apply(1))
        bridge.apply(.apply(4))
        XCTAssertEqual(bridge.resultText(), "Gửi Về Tp. Hcm")

        let state = bridge.state(
            input: "GỬI VỀ TP. HCM", charsets: [], transforms: ConvertWorker.loadTransforms()
        )
        XCTAssertEqual(state.appliedTransforms, [1, 4])

        bridge.apply(.undo)
        XCTAssertEqual(bridge.resultText(), "gửi về tp. hcm")
        bridge.apply(.clear)
        XCTAssertEqual(bridge.resultText(), "GỬI VỀ TP. HCM")
    }

    func testKeepDSwitchReachesCore() throws {
        let bridge = try XCTUnwrap(ConvertFFISession())
        bridge.setInput("đẹp")
        bridge.setSource(0)
        bridge.apply(.apply(2))
        XCTAssertEqual(bridge.resultText(), "dep")

        bridge.apply(.setKeepD(true))
        XCTAssertEqual(bridge.resultText(), "đep")
        XCTAssertTrue(bridge.state(input: "đẹp", charsets: [], transforms: []).keepD)
    }

    /// Typing is debounced 150 ms. A transform pressed before that lands must still
    /// run on what was typed — the reason casing goes through `updateFlushing`.
    func testATransformPressedRightAfterTypingSeesTheTypedText() async throws {
        let store = ConvertStore()

        store.send(.setInput("tôi yêu tiếng việt"))
        store.send(.casing(.apply(0)))

        try await waitUntil { store.state.outputText == "TÔI YÊU TIẾNG VIỆT" }
        XCTAssertEqual(store.state.appliedTransforms, [0])
    }

    private func scratchDirectory() throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appending(path: "funput-macos-convert-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    private func waitUntil(_ predicate: @escaping () -> Bool) async throws {
        for _ in 0..<100 where !predicate() {
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertTrue(predicate())
    }
}
