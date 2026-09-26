import XCTest

/// Typo correction through the real keyboard extension: a finger that lands on the
/// wrong key, and what the document ends up holding.
///
/// The tap positions are the whole point. Correction only has something to work with
/// when the touch says a neighbouring key was close, so the slipped key is tapped
/// near the edge it drifted from rather than at its centre — which is exactly what a
/// thumb does at speed.
///
/// Prerequisites (simulator): run `Scripts/uitest-enable-keyboard.sh` once after
/// installing the app. On a device, enable the keyboard in Settings first.
final class TypoCorrectionUITests: XCTestCase {
    /// `đường` in VNI is `d9u7o7ng2`; the finger lands on `h`, one key right of `g`.
    private let typed = "d9u7o7nh2"
    private let slipIndex = 7

    /// What those keys compose to when nothing repairs them.
    ///
    /// Not the raw keystrokes: the harness pins `eagerRestore = false` so its output
    /// stays deterministic, which leaves the composed form standing. The boundary does
    /// not restore it either — VNI spells with digits, so `keystrokes_intend_vietnamese`
    /// reads the word as deliberate.
    private let uncorrected = "đườnh"

    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        app?.terminate()
        let cleanup = XCUIApplication()
        cleanup.launchArguments = ["-uitest-clear-configuration-override"]
        cleanup.launch()
        cleanup.terminate()
        app = nil
    }

    @MainActor
    func testASlippedKeyIsRepairedWhenTheFieldAllowsAutocorrect() throws {
        let field = try harness(allowsAutocorrect: true)

        typeWithSlip()

        XCTAssertEqual(committedText(field), "đường ")
    }

    @MainActor
    func testAFieldThatRefusesAutocorrectKeepsWhatWasTyped() throws {
        // The same taps, the same slip: only the field's trait differs.
        let field = try harness(allowsAutocorrect: false)

        typeWithSlip()

        XCTAssertEqual(committedText(field), "\(uncorrected) ")
    }

    @MainActor
    func testBackspaceTakesTheRepairBack() throws {
        let field = try harness(allowsAutocorrect: true)
        typeWithSlip()
        XCTAssertEqual(committedText(field), "đường ", "nothing to undo otherwise")

        try key(labeled: "Xóa").tap()

        // What was on screen before the repair comes back, and with it the space the
        // user never touched.
        XCTAssertEqual(committedText(field), "\(uncorrected) ")
    }

    // MARK: - Harness

    @MainActor
    private func harness(allowsAutocorrect: Bool) throws -> XCUIElement {
        app = XCUIApplication()
        app.launchArguments += ["-uitest-typing-harness"]
        if allowsAutocorrect { app.launchArguments += ["-uitest-autocorrect"] }
        app.launch()

        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10), "typing harness field missing")
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app), """
        Funput keyboard never appeared. Enable the extension first: \
        simulator → Scripts/uitest-enable-keyboard.sh, device → Settings › \
        General › Keyboard › Keyboards.
        """)
        return field
    }

    /// Tap every key at its centre, except the slipped one — tapped a sixth of the way
    /// in from the edge nearest the key it was meant for, so the keyboard reports that
    /// key as the near neighbour.
    @MainActor
    private func typeWithSlip() {
        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: typed + " ")
        for (index, character) in typed.enumerated() {
            guard let frame = frames[character] else { continue }
            let x = index == slipIndex ? frame.minX + frame.width / 6 : frame.midX
            app.coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: x, dy: frame.midY))
                .tap()
        }
        if let space = frames[" "] {
            app.coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: space.midX, dy: space.midY))
                .tap()
        }
    }

    @MainActor
    private func key(labeled label: String) throws -> XCUICoordinate {
        let frame = try XCTUnwrap(
            FunputKeyboardDriver.keyFrame(app, labeled: label),
            "key \"\(label)\" not found"
        )
        return app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
    }

    @MainActor
    private func committedText(_ field: XCUIElement) -> String {
        // The correction lands in the same transaction as the space, but the proxy
        // still reports asynchronously.
        Thread.sleep(forTimeInterval: 1)
        return (field.value as? String) ?? ""
    }
}
