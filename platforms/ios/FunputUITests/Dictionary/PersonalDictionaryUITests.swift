import XCTest

/// The dictionary page against a seeded in-memory store: it opens from "Thông minh",
/// switches lists, blocks from the context menu and adds a word through the sheet.
final class PersonalDictionaryUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    private func openDictionary(seed: String = "samples") -> XCUIApplication {
        // The shortcuts test environment also gives settings throwaway stores.
        let app = ShortcutsUITestSupport.makeApp()
        app.launchEnvironment["FUNPUT_DICTIONARY_TEST_SEED"] = seed
        app.launch()
        let smartInput = app.buttons["settings.destination.smartInput"]
        XCTAssertTrue(smartInput.waitForExistence(timeout: 10))
        for _ in 0..<4 where !smartInput.isHittable { app.swipeUp() }
        smartInput.tap()
        let dictionary = app.buttons["settings.destination.personalDictionary"]
        XCTAssertTrue(dictionary.waitForExistence(timeout: 5))
        dictionary.tap()
        XCTAssertTrue(app.navigationBars["Từ điển cá nhân"].waitForExistence(timeout: 5))
        return app
    }

    @MainActor
    private func word(_ text: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any)["dictionary.word.\(text)"]
    }

    @MainActor
    func testBlockingMovesAWordToTheBlockedList() {
        let app = openDictionary()
        // The list is lazy: a row below the fold is not there until it is reached.
        let search = app.textFields["dictionary.search"]
        XCTAssertTrue(search.waitForExistence(timeout: 5))
        search.tap()
        search.typeText("deadline")
        let deadline = word("deadline", in: app)
        XCTAssertTrue(deadline.waitForExistence(timeout: 5))
        deadline.press(forDuration: 1)
        let block = app.buttons["Không gợi ý từ này"]
        XCTAssertTrue(block.waitForExistence(timeout: 5))
        block.tap()
        XCTAssertTrue(deadline.waitForNonExistence(timeout: 5))

        app.buttons["dictionary.search.clear"].tap()
        app.segmentedControls["dictionary.filter"].buttons.element(boundBy: 2).tap()
        XCTAssertTrue(word("deadline", in: app).waitForExistence(timeout: 5))
    }

    @MainActor
    func testAddingAWordExplainsSpacesThenSaves() {
        let app = openDictionary()
        app.buttons["dictionary.add"].tap()
        let field = app.textFields["dictionary.add.word"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.typeText("hai từ")
        XCTAssertTrue(app.descendants(matching: .any)["dictionary.add.problem"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["dictionary.add.save"].isEnabled)

        field.clearAndType("Shopee")
        XCTAssertTrue(app.descendants(matching: .any)["dictionary.add.preview"].waitForExistence(timeout: 5))
        app.buttons["dictionary.add.save"].tap()
        XCTAssertTrue(field.waitForNonExistence(timeout: 5))

        app.segmentedControls["dictionary.filter"].buttons.element(boundBy: 1).tap()
        XCTAssertTrue(word("shopee", in: app).waitForExistence(timeout: 5))
    }

    @MainActor
    func testAnEmptyDictionaryInvitesTheFirstWord() {
        let app = openDictionary(seed: "empty")
        XCTAssertTrue(app.descendants(matching: .any)["dictionary.empty.learned"].waitForExistence(timeout: 5))
        app.segmentedControls["dictionary.filter"].buttons.element(boundBy: 1).tap()
        XCTAssertTrue(app.buttons["dictionary.empty.add"].waitForExistence(timeout: 5))
    }
}

private extension XCUIElement {
    func clearAndType(_ text: String) {
        tap()
        let current = value as? String ?? ""
        typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count) + text)
    }
}
