import XCTest

@testable import Funput

/// Per-app VI/EN resolution (`AppSettings.resolveVietnamese`) — the macOS port of
/// the Windows shell's override map. A manual toggle must survive focus changes
/// instead of being reverted by the exclusion-list default.
@MainActor
final class VietnameseOverrideTests: XCTestCase {
    private func makeSettings() -> (AppSettings, () -> Void) {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        let settings = AppSettings(defaults: defaults)
        return (settings, { defaults.removePersistentDomain(forName: suiteName) })
    }

    func testExclusionDefaultAppliesWhenNothingIsPinned() {
        let (settings, cleanup) = makeSettings()
        defer { cleanup() }
        settings.excludedApps = [ExcludedApp(id: "dev.warp.Warp-Stable", name: "Warp")]

        XCTAssertEqual(settings.resolveVietnamese(for: "dev.warp.Warp-Stable"), false)
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.TextEdit"), true)
    }

    func testNoExclusionListAndNoPinLeavesStateUntouched() {
        let (settings, cleanup) = makeSettings()
        defer { cleanup() }
        XCTAssertNil(settings.resolveVietnamese(for: "com.apple.TextEdit"))
    }

    func testHotkeyPinSurvivesRefocus() {
        let (settings, cleanup) = makeSettings()
        defer { cleanup() }
        settings.excludedApps = [ExcludedApp(id: "dev.warp.Warp-Stable", name: "Warp")]

        // User toggles EN via the hotkey while TextEdit (non-excluded) is focused.
        settings.pinVietnamese(false, to: "com.apple.TextEdit")
        XCTAssertFalse(settings.vietnameseEnabled)

        // Refocusing TextEdit must keep EN — this was the "always snaps back to VI" bug.
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.TextEdit"), false)
        // Other apps still get the exclusion-list default.
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.Safari"), true)
    }

    func testUIChoiceBindsToTheNextFocusedApp() {
        let (settings, cleanup) = makeSettings()
        defer { cleanup() }
        settings.excludedApps = [ExcludedApp(id: "dev.warp.Warp-Stable", name: "Warp")]

        // User picks EN in the menu bar while Funput's own window holds focus.
        settings.setVietnameseFromUI(false)

        // The next focused app consumes the pending choice and pins it…
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.TextEdit"), false)
        // …so it sticks on later refocus, while other apps keep the default.
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.TextEdit"), false)
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.Safari"), true)
    }

    func testHotkeyPinDropsStalePendingUIChoice() {
        let (settings, cleanup) = makeSettings()
        defer { cleanup() }
        settings.setVietnameseFromUI(false)

        // A later hotkey toggle (VI) in Safari wins; the stale pending EN is dropped.
        settings.pinVietnamese(true, to: "com.apple.Safari")
        XCTAssertEqual(settings.resolveVietnamese(for: "com.apple.Safari"), true)
        XCTAssertNil(settings.pendingVietnameseOverride)
    }
}
