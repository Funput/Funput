import XCTest
@testable import Funput

final class ConvertStateTests: XCTestCase {
    func testEmptyStateUsesLiveCharsets() {
        let state = ConvertScreenState.empty(charsets: ConvertFixtures.charsets)

        XCTAssertEqual(state.mode, .empty)
        XCTAssertEqual(state.charsets, ConvertFixtures.charsets)
        XCTAssertFalse(state.canUseTextResult)
    }

    func testBatchRowsUseGlobalStableIdentifiers() {
        let rows = ConvertFixtures.batch.files

        XCTAssertEqual(rows.map(\.id), [0, 1, 2, 3])
        XCTAssertEqual(Set(rows.map(\.id)).count, rows.count)
    }

    func testResolvingBatchRowUpdatesReadyCount() {
        var state = ConvertFixtures.batch
        state.rowsTotal = 504

        XCTAssertTrue(state.canLoadMore)
        state.isBusy = true
        XCTAssertFalse(state.canLoadMore)
    }

    func testTextAndBatchActionsReflectAvailability() {
        var unresolved = ConvertFixtures.pasted
        unresolved.source = nil

        XCTAssertFalse(unresolved.canUseTextResult)
        XCTAssertEqual(ConvertFixtures.pasted.textPrimaryAction, "Lưu tệp…")
        XCTAssertEqual(ConvertFixtures.singleFile.textPrimaryAction, "Chuyển tệp")
        XCTAssertEqual(ConvertFixtures.batch.batchAction, "Chuyển 3 tệp")
        XCTAssertEqual(ConvertFixtures.busyBatch.batchAction, "Đang chuyển…")
    }

    func testUnknownBatchPlaceholderCannotReplaceASelectedCharset() {
        let store = ConvertStore(initialState: ConvertFixtures.batch)
        let initial = store.state

        store.send(.setRowSource(id: 0, source: nil))

        XCTAssertEqual(store.state, initial)
    }
}
