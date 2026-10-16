import XCTest

/// Typo correction on the suggestion bar: the repair shown before the word ends, and
/// the three things a tap on the bar can do with it.
final class TypoCorrectionBarUITests: XCTestCase {
    private var harness: TypoCorrectionHarness?
    private let typed = TypoCorrectionHarness.uncorrected
    private let repaired = TypoCorrectionHarness.repaired

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    override func tearDown() {
        TypoCorrectionHarness.cleanup(harness?.app)
        harness = nil
        super.tearDown()
    }

    @MainActor
    func testTheBarPreviewsTheRepairBeforeSpace() throws {
        let harness = try launch()

        harness.typeWithSlip(space: false)

        // The word as typed sits beside its repair, and nothing has been committed.
        XCTAssertTrue(
            harness.barSlot("Giữ nguyên, \(typed)").waitForExistence(timeout: 5),
            "bar shows \(harness.barLabels())"
        )
        XCTAssertTrue(harness.barSlot("Sửa thành, \(repaired)").exists)
        XCTAssertEqual(harness.committedText(), typed)
    }

    @MainActor
    func testTappingTheCandidateCommitsIt() throws {
        let harness = try launch()
        harness.typeWithSlip(space: false)
        let candidate = harness.barSlot("Sửa thành, \(repaired)")
        XCTAssertTrue(candidate.waitForExistence(timeout: 5), "bar shows \(harness.barLabels())")

        candidate.tap()

        // The repair and its space land together, and the undo chip takes over the bar.
        XCTAssertEqual(harness.committedText(), "\(repaired) ")
        XCTAssertTrue(harness.barSlot("Hoàn tác, \(typed)").waitForExistence(timeout: 5))
    }

    @MainActor
    func testTappingTheTypedWordKeepsIt() throws {
        let harness = try launch()
        harness.typeWithSlip(space: false)
        let keep = harness.barSlot("Giữ nguyên, \(typed)")
        XCTAssertTrue(keep.waitForExistence(timeout: 5), "bar shows \(harness.barLabels())")

        keep.tap()
        try harness.key(labeled: "Dấu cách", exact: false).tap()

        // Space no longer repairs the word, and the bar has nothing left to offer.
        XCTAssertEqual(harness.committedText(), "\(typed) ")
        XCTAssertFalse(harness.barSlot("Sửa thành, \(repaired)").exists)
    }

    @MainActor
    func testTheUndoChipTakesTheRepairBack() throws {
        let harness = try launch()
        harness.typeWithSlip()
        XCTAssertEqual(harness.committedText(), "\(repaired) ", "nothing to undo otherwise")
        let undo = harness.barSlot("Hoàn tác, \(typed)")
        XCTAssertTrue(undo.waitForExistence(timeout: 5), "bar shows \(harness.barLabels())")

        undo.tap()

        // Exactly what Backspace would do: the word as typed, and the space with it.
        XCTAssertEqual(harness.committedText(), "\(typed) ")
        XCTAssertFalse(undo.exists)
    }

    @MainActor
    func testAnUndecidedRepairStaysOnTheBarAfterSpace() throws {
        let harness = try launch()
        harness.typeDeadCentre()

        // Space committed the word as typed, and the bar still offers the repair.
        XCTAssertEqual(harness.committedText(), "\(typed) ")
        let candidate = harness.barSlot("Sửa thành, \(repaired)")
        XCTAssertTrue(candidate.waitForExistence(timeout: 5), "bar shows \(harness.barLabels())")
        XCTAssertTrue(harness.barSlot("Giữ nguyên, \(typed)").exists)

        candidate.tap()

        // The repair takes the word's place, the space stays, and the undo chip follows.
        XCTAssertEqual(harness.committedText(), "\(repaired) ")
        XCTAssertTrue(harness.barSlot("Hoàn tác, \(typed)").waitForExistence(timeout: 5))
    }

    @MainActor
    private func launch() throws -> TypoCorrectionHarness {
        let harness = try TypoCorrectionHarness(allowsAutocorrect: true)
        self.harness = harness
        return harness
    }
}
