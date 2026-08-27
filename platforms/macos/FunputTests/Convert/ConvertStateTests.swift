import XCTest
@testable import Funput

final class ConvertStateTests: XCTestCase {
    func testPrototypeStartsEmptyAndCanRestart() {
        var session = ConvertPrototypeSession(state: ConvertFixtures.pasted)

        session.send(.restart)

        XCTAssertEqual(session.state, ConvertFixtures.empty)
    }

    func testDebugActionsReachTextAndBatchFixtures() {
        var session = ConvertPrototypeSession()

        session.send(.paste)
        XCTAssertEqual(session.state.mode, .text)
        XCTAssertFalse(session.state.fromFile)

        session.send(.restart)
        session.send(.pickFiles)
        XCTAssertEqual(session.state.mode, .files)
        XCTAssertEqual(session.state.files.count, 4)
    }

    func testSelectionsFlowThroughSingleActionDoor() {
        var session = ConvertPrototypeSession(state: ConvertFixtures.pasted)

        session.send(.setSource(2))
        session.send(.setTarget(3))
        session.send(.setInput("Nội dung mới"))

        XCTAssertEqual(session.state.source, 2)
        XCTAssertEqual(session.state.target, 3)
        XCTAssertEqual(session.state.inputText, "Nội dung mới")
    }

    func testBatchRowsUseGlobalStableIdentifiers() {
        let rows = ConvertFixtures.batch.files

        XCTAssertEqual(rows.map(\.id), [0, 1, 2, 3])
        XCTAssertEqual(Set(rows.map(\.id)).count, rows.count)
    }

    func testResolvingBatchRowUpdatesReadyCount() {
        var session = ConvertPrototypeSession(state: ConvertFixtures.batch)

        session.send(.setRowSource(id: 2, source: 1))

        XCTAssertEqual(session.state.files[id: 2]?.source, 1)
        XCTAssertEqual(session.state.files[id: 2]?.note, "")
        XCTAssertEqual(session.state.ready, 4)
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
}

private extension Array where Element == ConvertFileRow {
    subscript(id id: Int) -> ConvertFileRow? {
        first(where: { $0.id == id })
    }
}
