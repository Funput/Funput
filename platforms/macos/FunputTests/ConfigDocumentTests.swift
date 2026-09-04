import XCTest
@testable import Funput

@MainActor
final class ConfigDocumentTests: XCTestCase {
    private func makeSettings() -> (AppSettings, String) {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        return (AppSettings(defaults: defaults), suiteName)
    }

    /// Both gõ tắt switches are portable preferences, so an export has to carry them
    /// and an import has to apply them. A field missing from `Preferences` decodes
    /// silently as `nil`, which is why this asserts the values rather than the shape.
    func testShortcutSwitchesSurviveExportAndImport() throws {
        let (source, sourceSuite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: sourceSuite) }
        source.shortcutsEnabled = false
        source.shortcutSmartCase = false

        let data = try source.exportData()

        let (destination, destinationSuite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: destinationSuite) }
        XCTAssertTrue(destination.shortcutsEnabled, "starts at the default")
        XCTAssertTrue(destination.shortcutSmartCase)

        try destination.importData(data)

        XCTAssertFalse(destination.shortcutsEnabled)
        XCTAssertFalse(destination.shortcutSmartCase)
    }

    /// Import is a merge: a file written before these fields existed must leave the
    /// local switches alone rather than resetting them to the format's default.
    func testAFileWithoutTheSwitchesLeavesLocalOnesAlone() throws {
        let (settings, suite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        settings.shortcutsEnabled = false
        settings.shortcutSmartCase = false

        let legacy = """
        { "schema": "app.funput.config", "version": 1,
          "preferences": { "inputMethod": "vni" } }
        """
        try settings.importData(Data(legacy.utf8))

        XCTAssertFalse(settings.shortcutsEnabled)
        XCTAssertFalse(settings.shortcutSmartCase)
    }
}
