import XCTest

/// The keyboard tab's tiles and input-method picker change settings in place.
final class QuickSettingsUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testTileFlipsItsSetting() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let tile = app.buttons["settings.quick.autoCapitalize"]
        XCTAssertTrue(tile.waitForExistence(timeout: 10))
        let before = tile.value as? String
        tile.tap()
        let flipped = NSPredicate(format: "value != %@", before ?? "")
        expectation(for: flipped, evaluatedWith: tile)
        waitForExpectations(timeout: 5)
    }

    @MainActor
    func testVNILocksTheNumberRowTile() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let picker = app.segmentedControls["settings.quick.inputMethod"]
        XCTAssertTrue(picker.waitForExistence(timeout: 10))
        picker.buttons["VNI"].tap()
        let numberRow = app.buttons["settings.quick.numberRow"]
        XCTAssertTrue(numberRow.waitForExistence(timeout: 5))
        XCTAssertEqual(numberRow.value as? String, "Luôn bật")
        XCTAssertFalse(numberRow.isEnabled)
        picker.buttons["Telex"].tap()
        let unlocked = NSPredicate(format: "isEnabled == true")
        expectation(for: unlocked, evaluatedWith: numberRow)
        waitForExpectations(timeout: 5)
    }
}
