import Foundation
import XCTest
@testable import Funput

final class ConvertBridgeTests: XCTestCase {
    func testTextUsesCoreResultAndExactLegacyBytes() throws {
        let bridge = try XCTUnwrap(ConvertFFISession())
        bridge.setInput("Việt")
        bridge.setSource(0)
        bridge.setTarget(1)

        let state = bridge.state(input: "Việt", charsets: ConvertWorker.loadCharsets())

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
        let first = bridge.state(input: "", charsets: [])
        XCTAssertEqual(first.rowsTotal, 501)
        XCTAssertEqual(first.files.count, 500)
        XCTAssertEqual(first.files.first?.id, 0)
        XCTAssertEqual(first.files.last?.id, 499)

        bridge.setRowCount(1_000)
        let expanded = bridge.state(input: "", charsets: [])
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
        let state = bridge.state(input: "", charsets: [])
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
