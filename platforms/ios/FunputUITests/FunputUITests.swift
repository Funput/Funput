import XCTest

final class FunputUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testThemeEditorScrollAndControlsRemainResponsive() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Giao diện"].tap()

        let customize = app.buttons["appearance.customize"]
        XCTAssertTrue(customize.waitForExistence(timeout: 5))
        customize.tap()
        XCTAssertTrue(app.buttons["themeEditor.save"].waitForExistence(timeout: 5))
        let preview = app.otherElements["themeEditor.preview"]
        XCTAssertTrue(preview.waitForExistence(timeout: 5))
        let previewFrame = preview.frame

        let generalScroll = app.scrollViews["themeEditor.scroll.general"]
        XCTAssertTrue(generalScroll.waitForExistence(timeout: 5))
        generalScroll.swipeUp()
        XCTAssertEqual(preview.frame.minY, previewFrame.minY, accuracy: 1)

        app.segmentedControls["themeEditor.tabs"].buttons["Khi nhấn"].tap()
        let pressedToggle = app.switches["themeEditor.pressedOverlayEnabled"]
        XCTAssertTrue(pressedToggle.waitForExistence(timeout: 5))
        pressedToggle.tap()

        let scale = app.sliders["themeEditor.pressedScale"]
        XCTAssertTrue(scale.isHittable)
        scale.adjust(toNormalizedSliderPosition: 0.5)

        app.segmentedControls["themeEditor.tabs"].buttons["Chung"].tap()
        XCTAssertTrue(app.textFields["themeEditor.name"].exists)
        generalScroll.swipeDown()
        XCTAssertTrue(app.textFields["themeEditor.name"].isHittable)
        XCTAssertEqual(preview.frame.minY, previewFrame.minY, accuracy: 1)
        XCTAssertTrue(app.buttons["themeEditor.save"].isEnabled)
    }

    @MainActor
    func testLaunchPerformance() throws {
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
