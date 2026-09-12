import XCTest
@testable import Funput

final class ConvertStateTests: XCTestCase {
    func testEmptyStateUsesLiveCharsets() {
        let state = ConvertScreenState.empty(
            charsets: ConvertFixtures.charsets, transforms: ConvertFixtures.transforms
        )

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

    /// The order is the feature: the same two transforms the other way round is a
    /// different document, so the line the window shows has to keep it.
    func testAppliedTransformsReadBackInThePressedOrder() {
        XCTAssertEqual(
            ConvertFixtures.cased.appliedTransformNames,
            ["chữ thường", "Viết Hoa Đầu Mỗi Từ"]
        )
        XCTAssertTrue(ConvertFixtures.pasted.appliedTransformNames.isEmpty)
    }

    /// A switch belongs to one transform and only appears while that one is applied.
    func testSwitchesAppearOnlyWithTheTransformTheyBelongTo() {
        var state = ConvertFixtures.cased

        XCTAssertTrue(state.showsSwitch(for: .title))
        XCTAssertFalse(state.showsSwitch(for: .noDiacritics))

        state.appliedTransforms = [ConvertTransformKind.noDiacritics.position]
        XCTAssertTrue(state.showsSwitch(for: .noDiacritics))
        XCTAssertFalse(state.showsSwitch(for: .title))
    }

    /// One rule for both axes: nothing is converted, and nothing is transformed,
    /// until something explains the document. A batch explains itself per row.
    func testCasingIsBlockedUntilTheDocumentIsExplained() {
        var unresolved = ConvertFixtures.pasted
        unresolved.source = nil
        XCTAssertFalse(unresolved.canUseCasing)

        XCTAssertTrue(ConvertFixtures.pasted.canUseCasing)
        XCTAssertTrue(ConvertFixtures.batch.canUseCasing, "a batch carries a charset per row")
        XCTAssertFalse(ConvertFixtures.busyBatch.canUseCasing)
    }

    func testUnknownBatchPlaceholderCannotReplaceASelectedCharset() {
        let store = ConvertStore(initialState: ConvertFixtures.batch)
        let initial = store.state

        store.send(.setRowSource(id: 0, source: nil))

        XCTAssertEqual(store.state, initial)
    }
}
