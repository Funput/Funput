import XCTest

/// The news tab lists the feed and opens a post in Safari.
final class NewsUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testArticleOpensInSafari() {
        let app = XCUIApplication()
        app.launchEnvironment["FUNPUT_NEWS_TEST_FIXTURE"] = "1"
        app.launch()
        let tab = app.tabBars.buttons["Tin tức"]
        XCTAssertTrue(tab.waitForExistence(timeout: 10))
        tab.tap()
        XCTAssertTrue(app.buttons["news.guide.setup"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["news.guide.typing"].exists)
        let article = app.buttons["news.article.telex-vni-va-telex-nang-cao"]
        XCTAssertTrue(article.waitForExistence(timeout: 5))
        article.tap()
        // The Safari sheet covers the tab bar once it is up.
        let covered = NSPredicate(format: "isHittable == false")
        expectation(for: covered, evaluatedWith: tab)
        waitForExpectations(timeout: 5)
    }
}
