import XCTest

@testable import Funput

/// Per-app VI/EN memory (`AppLanguageMemory`, backed by `funput-ffi`'s
/// `FunputAppLanguage`). A manual toggle must survive focus changes — and,
/// unlike the old session-only override map, must survive a relaunch too.
final class AppLanguageMemoryTests: XCTestCase {
    private func makeMemory() -> (AppLanguageMemory, UserDefaults, () -> Void) {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        let memory = AppLanguageMemory(defaults: defaults)
        return (memory, defaults, { defaults.removePersistentDomain(forName: suiteName) })
    }

    func testUnseenAppWithNoPendingLeavesStateUntouched() {
        let (memory, _, cleanup) = makeMemory()
        defer { cleanup() }
        XCTAssertNil(memory.resolve(for: "com.apple.TextEdit"))
    }

    func testHotkeyPinSurvivesRefocus() {
        let (memory, _, cleanup) = makeMemory()
        defer { cleanup() }

        // User toggles EN via the hotkey while TextEdit is focused.
        memory.pin(false, to: "com.apple.TextEdit")

        // Refocusing TextEdit must keep EN — this was the "always snaps back to VI" bug.
        XCTAssertEqual(memory.resolve(for: "com.apple.TextEdit"), false)
        // Other apps are unaffected.
        XCTAssertNil(memory.resolve(for: "com.apple.Safari"))
    }

    func testUIChoiceBindsToTheNextFocusedApp() {
        let (memory, _, cleanup) = makeMemory()
        defer { cleanup() }

        // User picks EN in the menu bar while Funput's own window holds focus.
        memory.setPending(false)

        // The next focused app consumes the pending choice and remembers it…
        XCTAssertEqual(memory.resolve(for: "com.apple.TextEdit"), false)
        // …so it sticks on later refocus, while other apps stay untouched.
        XCTAssertEqual(memory.resolve(for: "com.apple.TextEdit"), false)
        XCTAssertNil(memory.resolve(for: "com.apple.Safari"))
    }

    func testHotkeyPinDropsStalePendingUIChoice() {
        let (memory, _, cleanup) = makeMemory()
        defer { cleanup() }
        memory.setPending(false)

        // A later hotkey toggle (VI) in Safari wins; the stale pending EN is dropped.
        memory.pin(true, to: "com.apple.Safari")
        XCTAssertEqual(memory.resolve(for: "com.apple.Safari"), true)
        XCTAssertNil(memory.pending)
    }

    func testRememberedChoicePersistsAcrossRelaunch() {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        AppLanguageMemory(defaults: defaults).pin(false, to: "com.apple.TextEdit")

        // A fresh instance over the same `UserDefaults` simulates a relaunch: it
        // must load the persisted choice, not start with an empty memory.
        let relaunched = AppLanguageMemory(defaults: defaults)
        XCTAssertEqual(relaunched.resolve(for: "com.apple.TextEdit"), false)
    }

    func testLegacyExcludedAppsMigrateOnceToEnglish() {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let legacyJSON = #"[{"id":"dev.warp.Warp-Stable","name":"Warp"}]"#
        defaults.set(Data(legacyJSON.utf8), forKey: "excludedApps")

        let memory = AppLanguageMemory(defaults: defaults)
        XCTAssertEqual(memory.resolve(for: "dev.warp.Warp-Stable"), false)
        // The legacy key is consumed so the migration only ever runs once.
        XCTAssertNil(defaults.data(forKey: "excludedApps"))
    }

    func testMergeKeepsExistingChoiceOverIncoming() {
        let (memory, _, cleanup) = makeMemory()
        defer { cleanup() }
        memory.pin(true, to: "com.apple.TextEdit")

        memory.merge(["com.apple.TextEdit": false, "com.apple.Safari": false])

        XCTAssertEqual(memory.resolve(for: "com.apple.TextEdit"), true) // existing wins
        XCTAssertEqual(memory.resolve(for: "com.apple.Safari"), false) // new entry applied
    }
}
