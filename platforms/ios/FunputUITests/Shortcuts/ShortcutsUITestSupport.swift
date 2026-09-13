import XCTest

@MainActor
enum ShortcutsUITestSupport {
    static func makeApp(seed: Bool = false, failure: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] = UUID().uuidString
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_SEED"] = seed ? "1" : "0"
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_FAILURE"] = failure
        return app
    }

    static func open(_ app: XCUIApplication) {
        let entry = app.buttons["settings.shortcuts"]
        XCTAssertTrue(entry.waitForExistence(timeout: 10))
        for _ in 0..<12 where !entry.isHittable { app.swipeUp() }
        entry.tap()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 5))
    }

    static func waitForValue(_ element: XCUIElement, _ value: String) {
        let expectation = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", value), object: element)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 5), .completed)
    }

    static func assertKeyboardVisible(_ app: XCUIApplication) {
        let visible = NSPredicate { _, _ in
            app.keyboards.firstMatch.exists || app.keys.matching(
                NSPredicate(format: "label BEGINSWITH %@", "Dấu cách. Vuốt")
            ).firstMatch.exists
        }
        let expectation = XCTNSPredicateExpectation(predicate: visible, object: app)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 5), .completed)
    }

    static func capture(_ app: XCUIApplication, name: String, test: XCTestCase) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        test.add(attachment)
    }
}
