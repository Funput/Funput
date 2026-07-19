import XCTest

final class FunputUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testThemeEditorScrollAndControlsRemainResponsive() throws {
        let app = XCUIApplication()
        app.launch()
        openAppearance(in: app)
        XCTAssertTrue(app.staticTexts["Hệ thống"].waitForExistence(timeout: 5))

        let customize = app.buttons["appearance.customize"]
        XCTAssertTrue(customize.waitForExistence(timeout: 5))
        customize.tap()
        XCTAssertTrue(app.buttons["themeEditor.save"].waitForExistence(timeout: 5))
        let preview = app.otherElements["themeEditor.preview"]
        XCTAssertTrue(preview.waitForExistence(timeout: 5))
        let previewFrame = preview.frame
        let previewKey = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == 'a'"))
            .firstMatch
        XCTAssertTrue(previewKey.waitForExistence(timeout: 5))
        previewKey.tap()
        XCTAssertTrue(app.buttons["themeEditor.save"].exists)

        let generalScroll = app.scrollViews["themeEditor.scroll.general"]
        XCTAssertTrue(generalScroll.waitForExistence(timeout: 5))
        generalScroll.swipeUp()
        XCTAssertEqual(preview.frame.minY, previewFrame.minY, accuracy: 1)

        let tabs = app.segmentedControls["themeEditor.tabs"]
        tabs.buttons["Nền"].tap()
        let backgroundScroll = app.scrollViews["themeEditor.scroll.background"]
        let backgroundMode = app.segmentedControls["themeEditor.backgroundMode"]
        XCTAssertTrue(backgroundMode.waitForExistence(timeout: 5))
        backgroundMode.buttons["Gradient"].tap()
        let material = app.segmentedControls["themeEditor.material"]
        XCTAssertTrue(material.waitForExistence(timeout: 5))
        material.buttons["Liquid Glass"].tap()
        tabs.buttons["Phím & chữ"].tap()
        let keyScroll = app.scrollViews["themeEditor.scroll.keys"]
        let shadowToggle = app.switches["themeEditor.glassShadowOverride"]
        for _ in 0..<6 where !shadowToggle.isHittable { keyScroll.swipeUp() }
        XCTAssertTrue(shadowToggle.isHittable)
        shadowToggle.tap()
        let shadowRadius = app.sliders["themeEditor.shadowRadius"]
        XCTAssertTrue(shadowRadius.isHittable)
        shadowRadius.adjust(toNormalizedSliderPosition: 0.5)

        tabs.buttons["Nền"].tap()
        material.buttons["Solid"].tap()
        material.buttons["Liquid Glass"].tap()
        let direction = app.segmentedControls["themeEditor.gradientDirection"]
        for _ in 0..<8 where !direction.isHittable { backgroundScroll.swipeUp() }
        XCTAssertTrue(direction.isHittable)
        direction.buttons["Dọc"].tap()
        let backgroundOpacity = app.sliders["themeEditor.backgroundStartOpacity"]
        for _ in 0..<2 where !backgroundOpacity.isHittable { backgroundScroll.swipeUp() }
        XCTAssertTrue(backgroundOpacity.isHittable)
        backgroundOpacity.adjust(toNormalizedSliderPosition: 0.5)
        for _ in 0..<6 where !backgroundMode.isHittable { backgroundScroll.swipeDown() }
        backgroundMode.buttons["Ảnh"].tap()
        let imagePicker = app.buttons["themeEditor.imagePicker"]
        for _ in 0..<4 where !imagePicker.isHittable { backgroundScroll.swipeUp() }
        XCTAssertTrue(imagePicker.isHittable)
        for _ in 0..<4 where !backgroundMode.isHittable { backgroundScroll.swipeDown() }
        backgroundMode.buttons["Gradient"].tap()
        tabs.buttons["Phím & chữ"].tap()
        XCTAssertTrue(shadowToggle.isHittable)
        XCTAssertEqual(preview.frame.minY, previewFrame.minY, accuracy: 1)

        tabs.buttons["Khi nhấn"].tap()
        let pressedToggle = app.switches["themeEditor.pressedOverlayEnabled"]
        XCTAssertTrue(pressedToggle.waitForExistence(timeout: 5))
        pressedToggle.tap()

        let scale = app.sliders["themeEditor.pressedScale"]
        XCTAssertTrue(scale.isHittable)
        scale.adjust(toNormalizedSliderPosition: 0.5)

        tabs.buttons["Chung"].tap()
        XCTAssertTrue(app.textFields["themeEditor.name"].exists)
        generalScroll.swipeDown()
        XCTAssertTrue(app.textFields["themeEditor.name"].isHittable)
        XCTAssertEqual(preview.frame.minY, previewFrame.minY, accuracy: 1)
        XCTAssertTrue(app.buttons["themeEditor.save"].isEnabled)
    }

    @MainActor
    func testThemeGalleryScrollsAndHidesEmptyCustomSection() throws {
        let app = XCUIApplication()
        app.launch()
        openAppearance(in: app)
        XCTAssertTrue(app.staticTexts["Hệ thống"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["Của bạn"].exists)

        let carousel = app.scrollViews["appearance.carousel.system"]
        XCTAssertTrue(carousel.waitForExistence(timeout: 5))
        let firstTheme = app.buttons["appearance.theme.app.funput.theme.glass"]
        XCTAssertTrue(firstTheme.waitForExistence(timeout: 5))
        let initialX = firstTheme.frame.minX
        carousel.swipeLeft()
        XCTAssertLessThan(firstTheme.frame.minX, initialX - 20)
    }

    private func openAppearance(in app: XCUIApplication) {
        let tab = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", "Giao diện"))
            .firstMatch
        XCTAssertTrue(tab.waitForExistence(timeout: 5))
        tab.tap()
    }

    @MainActor
    func testLaunchPerformance() throws {
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
