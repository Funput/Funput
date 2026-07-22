import XCTest
@testable import Funput

@MainActor
final class ComposerConfigurationTests: XCTestCase {
    func testSettingsSnapshotMapsEveryComposerPreference() {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let settings = AppSettings(defaults: defaults)
        settings.inputMethod = .telexAdvanced
        settings.toneStyle = .modern
        settings.vietnameseEnabled = false
        settings.smartEnglishRestore = false
        settings.eagerRestore = false
        settings.spellCheckEnabled = true
        settings.autoCapitalizeEnabled = true

        let configuration = ComposerConfiguration(settings: settings)

        XCTAssertEqual(configuration.inputMethod, .telexAdvanced)
        XCTAssertEqual(configuration.toneStyle, .modern)
        XCTAssertFalse(configuration.enabled)
        XCTAssertFalse(configuration.smartEnglishRestore)
        XCTAssertFalse(configuration.eagerRestore)
        XCTAssertTrue(configuration.spellCheckEnabled)
        XCTAssertTrue(configuration.autoCapitalizeEnabled)
    }
}
