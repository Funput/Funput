import XCTest

final class AppearancePreviewDockUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testCornerButtonOpensTheDockAndTheDockClosesItself() throws {
        let app = XCUIApplication()
        app.launch()
        app.openAppearanceTab()

        let show = app.buttons["appearance.showPreview"]
        let hide = app.buttons["appearance.hidePreview"]
        XCTAssertTrue(show.waitForExistence(timeout: 5))
        XCTAssertFalse(hide.exists)

        show.tap()
        XCTAssertTrue(hide.waitForExistence(timeout: 5))
        XCTAssertTrue(app.otherElements["appearance.previewDock"].exists)
        XCTAssertTrue(app.segmentedControls["appearance.previewMode"].exists)
        XCTAssertTrue(show.waitForNonExistence(timeout: 5))
        // The gallery row stays reachable above the dock.
        XCTAssertTrue(app.staticTexts["appearance.section.system"].isHittable)

        hide.tap()
        XCTAssertTrue(show.waitForExistence(timeout: 5))
        XCTAssertTrue(hide.waitForNonExistence(timeout: 5))
    }

    @MainActor
    func testChoosingAnotherThemeOpensTheDockWithApply() throws {
        let app = XCUIApplication()
        app.launch()
        app.openAppearanceTab()

        let carousel = app.scrollViews["appearance.carousel.system"]
        XCTAssertTrue(carousel.waitForExistence(timeout: 5))
        carousel.swipeLeft()
        let unappliedTheme = carousel.buttons
            .matching(NSPredicate(format: "identifier BEGINSWITH 'appearance.theme.' AND value != 'Đang sử dụng'"))
            .firstMatch
        XCTAssertTrue(unappliedTheme.waitForExistence(timeout: 5))
        unappliedTheme.tap()

        XCTAssertTrue(app.buttons["appearance.hidePreview"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["appearance.apply"].waitForExistence(timeout: 5))
        XCTAssertEqual(unappliedTheme.value as? String, "Đang xem trước")
    }
}

extension XCUIApplication {
    func openAppearanceTab() {
        let tab = descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", "Giao diện"))
            .firstMatch
        XCTAssertTrue(tab.waitForExistence(timeout: 5))
        tab.tap()
    }
}
