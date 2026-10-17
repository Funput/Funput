import XCTest

/// Typo correction through the real keyboard extension: a finger that lands on the
/// wrong key, and what the document ends up holding when the word ends.
final class TypoCorrectionUITests: XCTestCase {
    private var harness: TypoCorrectionHarness?

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
    func testASlippedKeyIsRepairedWhenTheFieldAllowsAutocorrect() throws {
        let harness = try launch(allowsAutocorrect: true)

        harness.typeWithSlip()

        XCTAssertEqual(harness.committedText(), "\(TypoCorrectionHarness.repaired) ")
    }

    @MainActor
    func testAFieldThatRefusesAutocorrectKeepsWhatWasTyped() throws {
        // The same taps, the same slip: only the field's trait differs.
        let harness = try launch(allowsAutocorrect: false)

        harness.typeWithSlip()

        XCTAssertEqual(harness.committedText(), "\(TypoCorrectionHarness.uncorrected) ")
    }

    @MainActor
    func testBackspaceTakesTheRepairBack() throws {
        let harness = try launch(allowsAutocorrect: true)
        harness.typeWithSlip()
        XCTAssertEqual(
            harness.committedText(), "\(TypoCorrectionHarness.repaired) ", "nothing to undo otherwise"
        )

        try harness.key(labeled: "Xóa").tap()

        // What was on screen before the repair comes back, and with it the space the
        // user never touched.
        XCTAssertEqual(harness.committedText(), "\(TypoCorrectionHarness.uncorrected) ")
    }

    @MainActor
    private func launch(allowsAutocorrect: Bool) throws -> TypoCorrectionHarness {
        let harness = try TypoCorrectionHarness(allowsAutocorrect: allowsAutocorrect)
        self.harness = harness
        return harness
    }
}
