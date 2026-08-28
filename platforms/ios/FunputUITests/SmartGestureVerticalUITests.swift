import XCTest

/// Vertical caret panning through the real keyboard extension.
///
/// Kept apart from `SmartGestureUITests` because it needs a document with more than one
/// line, which none of the horizontal gestures care about.
///
/// Prerequisites are the same: the extension must already be enabled (simulator →
/// `Scripts/uitest-enable-keyboard.sh`, device → Settings › General › Keyboard ›
/// Keyboards).
final class SmartGestureVerticalUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        app?.terminate()
        let cleanupApp = XCUIApplication()
        cleanupApp.launchArguments = ["-uitest-clear-configuration-override"]
        cleanupApp.launch()
        cleanupApp.terminate()
        app = nil
    }

    @MainActor
    func testDraggingUpOnSpaceMovesTheCaretToTheLineAbove() throws {
        let field = try startTyping()
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: "abcdefx")
        type("abcdef", taps: taps)
        try key(labeled: "Enter").tap()
        type("abcdef", taps: taps)

        let space = try key(labeled: "Dấu cách", exact: false)
        // Past the 0.35s hold, then two 24pt line steps upward.
        space.press(
            forDuration: 0.45,
            thenDragTo: space.withOffset(CGVector(dx: 0, dy: -60)),
            withVelocity: .slow,
            thenHoldForDuration: 0.2
        )
        taps["x"]!.tap()

        Thread.sleep(forTimeInterval: 1)
        let actual = (field.value as? String) ?? ""
        let lines = actual.split(separator: "\n", omittingEmptySubsequences: false)
        XCTAssertEqual(lines.count, 2, "the drag lost or added a line: \(actual)")
        XCTAssertTrue(
            lines[0].contains("x"),
            "the caret never reached the line above: \(actual)"
        )
        XCTAssertEqual(actual.count, 14, "the drag inserted or ate characters: \(actual)")
    }

    @MainActor
    private func startTyping() throws -> XCUIElement {
        app = XCUIApplication()
        app.launchArguments += ["-uitest-typing-harness"]
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

    @MainActor
    private func key(labeled label: String, exact: Bool = true) throws -> XCUICoordinate {
        let frame = try XCTUnwrap(
            FunputKeyboardDriver.keyFrame(app, labeled: label, exact: exact),
            "key \"\(label)\" not found on the Funput keyboard"
        )
        return app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
    }

    @MainActor
    private func type(_ text: String, taps: [Character: XCUICoordinate]) {
        for character in text {
            taps[character]!.tap()
            usleep(80_000)
        }
    }
}
