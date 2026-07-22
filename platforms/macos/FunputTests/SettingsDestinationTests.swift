import XCTest
@testable import Funput

final class SettingsDestinationTests: XCTestCase {
    func testSettingsHasExpectedDestinations() {
        XCTAssertEqual(
            SettingsDestination.allCases,
            [.overview, .typing, .keyboardShortcuts, .textShortcuts, .applications]
        )
    }

    func testEveryDestinationHasNavigationMetadata() {
        for destination in SettingsDestination.allCases {
            XCTAssertFalse(destination.title.isEmpty)
            XCTAssertFalse(destination.subtitle.isEmpty)
            XCTAssertFalse(destination.systemImage.isEmpty)
        }
    }

    func testSidebarGroupsCoverEveryDestinationExactlyOnce() {
        let grouped = SettingsDestination.general
            + SettingsDestination.vietnameseTyping
            + SettingsDestination.automation

        XCTAssertEqual(grouped.count, SettingsDestination.allCases.count)
        XCTAssertEqual(Set(grouped), Set(SettingsDestination.allCases))
    }
}
