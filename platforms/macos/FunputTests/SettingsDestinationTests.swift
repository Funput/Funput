import XCTest
@testable import Funput

final class SettingsDestinationTests: XCTestCase {
    func testControlCenterHasExactlyFourDestinations() {
        XCTAssertEqual(
            SettingsDestination.allCases,
            [.overview, .typing, .automation, .shortcuts]
        )
    }

    func testEveryDestinationHasNavigationMetadata() {
        for destination in SettingsDestination.allCases {
            XCTAssertFalse(destination.title.isEmpty)
            XCTAssertFalse(destination.subtitle.isEmpty)
            XCTAssertFalse(destination.systemImage.isEmpty)
        }
    }
}
