import XCTest

/// Drives the installed keyboard extension at the physical screen edges.
final class KeyboardEdgeHitUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uitest-typing-harness"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app?.terminate()
        app = nil
    }

    @MainActor
    func testAlphabeticRowOwnsBothScreenEdges() throws {
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))

        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: "xabl")
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let offsets: [CGFloat] = [1, 4, 8, 12, 16, 20, 24, 25, 26, 28, 32, 36, 40]
        let leftXs = offsets
        let rightXs = offsets.map { app.frame.maxX - $0 }

        for x in leftXs {
            tap(x: x, y: frames["a"]!.midY, from: origin)
            tapCenter(of: frames["b"]!, from: origin)
        }
        for x in rightXs {
            tap(x: x, y: frames["l"]!.midY, from: origin)
            tapCenter(of: frames["b"]!, from: origin)
        }

        let expected = String(repeating: "ab", count: leftXs.count)
            + String(repeating: "lb", count: rightXs.count)
        XCTAssertEqual(field.value as? String, expected, """
        app=\(app.frame), a=\(frames["a"]!), l=\(frames["l"]!), \
        left=\(leftXs), right=\(rightXs)
        """)
    }

    private func tapCenter(of frame: CGRect, from origin: XCUICoordinate) {
        tap(x: frame.midX, y: frame.midY, from: origin)
    }

    private func tap(x: CGFloat, y: CGFloat, from origin: XCUICoordinate) {
        origin.withOffset(CGVector(dx: x, dy: y)).tap()
    }
}
