import XCTest

/// The three smart gestures driven through the real keyboard extension.
///
/// Only a real touch sequence exercises the whole stack — hold timers, the
/// gesture claim, and the document writes — so these live here rather than in
/// the unit targets, which stop at the controller boundary.
///
/// Prerequisites are the same as `VietnameseTypingUITests`: the extension must
/// already be enabled (simulator → `Scripts/uitest-enable-keyboard.sh`, device →
/// Settings › General › Keyboard › Keyboards).
final class SmartGestureUITests: XCTestCase {
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
    func testDoubleTappingSpaceWritesAFullStop() throws {
        let field = try startTyping()
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: "xin chao")
        type("xin chao", taps: taps)

        taps[" "]!.doubleTap()

        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(field.value as? String, "xin chao. ")
    }

    @MainActor
    func testHoldingSpaceAndDraggingMovesTheCaret() throws {
        let field = try startTyping()
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: "abcdefx")
        type("abcdef", taps: taps)

        let space = try key(labeled: "Dấu cách", exact: false)
        space.press(
            forDuration: 0.45,
            thenDragTo: space.withOffset(CGVector(dx: -40, dy: 0)),
            withVelocity: .slow,
            thenHoldForDuration: 0.2
        )
        taps["x"]!.tap()

        Thread.sleep(forTimeInterval: 1)
        let actual = (field.value as? String) ?? ""
        XCTAssertTrue(actual.contains("x"), "nothing was typed: \(actual)")
        XCTAssertFalse(
            actual.hasSuffix("x"),
            "the caret never moved — dragging the spacebar typed at the end: \(actual)"
        )
        XCTAssertEqual(actual.count, 7, "the drag inserted or ate characters: \(actual)")
    }

    @MainActor
    func testSwipingLeftOnBackspaceDeletesAWholeWord() throws {
        let field = try startTyping()
        type("xin chao", taps: FunputKeyboardDriver.resolveKeyCoordinates(app, for: "xin chao"))

        let backspace = try key(labeled: "Xóa")
        backspace.press(
            forDuration: 0.05,
            thenDragTo: backspace.withOffset(CGVector(dx: -70, dy: 0)),
            withVelocity: .slow,
            thenHoldForDuration: 0.2
        )

        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(field.value as? String, "xin ")
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
