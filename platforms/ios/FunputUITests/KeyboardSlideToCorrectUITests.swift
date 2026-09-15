import XCTest

/// Pins the key a slide commits to what Apple's keyboard does on iOS 27: the key under the
/// finger at lift, switching as soon as the finger crosses into the neighbour. See
/// `docs/KEY_ACCURACY_INVESTIGATION.md` §3.1 for the stock measurements this mirrors.
final class KeyboardSlideToCorrectUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
        app.launchArguments = ["-uitest-typing-harness"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app?.terminate()
        app = nil
    }

    @MainActor
    func testSlideCommitsTheKeyUnderTheLift() throws {
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))

        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: "hjyn ")
        let h = frames["h"]!, j = frames["j"]!, y = frames["y"]!
        let n = frames["n"]!, space = frames[" "]!
        // Row gaps are split at their midpoint, so this is where `h` hands over to `j`.
        let boundary = (h.maxX + j.minX) / 2

        slide(from: CGPoint(x: h.midX, y: h.midY), to: CGPoint(x: boundary - 3, y: h.midY))
        slide(from: CGPoint(x: h.midX, y: h.midY), to: CGPoint(x: boundary + 3, y: h.midY))
        slide(from: CGPoint(x: h.midX, y: h.midY), to: CGPoint(x: j.midX, y: j.midY))
        slide(from: CGPoint(x: h.midX, y: h.midY), to: CGPoint(x: y.midX, y: y.midY))
        // The spacebar stays a space when the thumb drifts up into the letter row.
        slide(from: CGPoint(x: n.midX, y: space.midY), to: CGPoint(x: n.midX, y: n.midY))

        XCTAssertEqual(field.value as? String, "hjjy ", """
        h=\(h), j=\(j), y=\(y), n=\(n), space=\(space), boundary=\(boundary)
        """)
    }

    private func slide(from start: CGPoint, to end: CGPoint) {
        let origin = app.coordinate(withNormalizedOffset: .zero)
        origin.withOffset(CGVector(dx: start.x, dy: start.y)).press(
            forDuration: 0.05,
            thenDragTo: origin.withOffset(CGVector(dx: end.x, dy: end.y)),
            withVelocity: XCUIGestureVelocity(200),
            thenHoldForDuration: 0.02
        )
        Thread.sleep(forTimeInterval: 0.3)
    }
}
