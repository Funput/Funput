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
        settings.shortcutsEnabled = false
        settings.shortcutSmartCase = false

        let configuration = ComposerConfiguration(settings: settings)

        XCTAssertEqual(configuration.inputMethod, .telexAdvanced)
        XCTAssertEqual(configuration.toneStyle, .modern)
        XCTAssertFalse(configuration.enabled)
        XCTAssertFalse(configuration.smartEnglishRestore)
        XCTAssertFalse(configuration.eagerRestore)
        XCTAssertTrue(configuration.spellCheckEnabled)
        XCTAssertTrue(configuration.autoCapitalizeEnabled)
        XCTAssertFalse(configuration.shortcutsEnabled)
        XCTAssertFalse(configuration.shortcutSmartCase)
    }

    /// Both gõ tắt switches are on by default, which only holds because their keys are
    /// in `defaults.register` — `UserDefaults.bool` reads a missing key as `false`, so
    /// forgetting a line there would silently kill everyone's shortcuts on update.
    func testShortcutSwitchesDefaultToOn() {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let settings = AppSettings(defaults: defaults)

        XCTAssertTrue(settings.shortcutsEnabled)
        XCTAssertTrue(settings.shortcutSmartCase)

        let configuration = ComposerConfiguration(settings: settings)
        XCTAssertTrue(configuration.shortcutsEnabled)
        XCTAssertTrue(configuration.shortcutSmartCase)
    }
}
