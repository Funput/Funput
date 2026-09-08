import XCTest
@testable import Funput

@MainActor
final class EnglishShortcutsTests: XCTestCase {
    func testEnglishExpansionUsesRawKeysAcrossMethodsAndBoundaries() {
        for method in InputMethod.allCases {
            for boundary: Unicode.Scalar in [" ", ".", "[", "]", "\t", "\r", "\u{00a0}"] {
                let composer = FunputComposer()
                composer.apply(.init(inputMethod: method, enabled: false, autoCapitalizeEnabled: true))
                composer.addShortcut(trigger: "addr1", expansion: "địa chỉ")
                composer.armCapitalization()
                for key in "addr1".unicodeScalars {
                    XCTAssertEqual(composer.process(key, source: .numpad).action, UInt8(ACTION_NONE))
                }
                XCTAssertEqual(composer.buffer(), "addr1")
                XCTAssertEqual(composer.flipComposing().action, UInt8(ACTION_NONE))
                let result = composer.process(boundary)
                XCTAssertEqual(result.action, UInt8(ACTION_SEND))
                XCTAssertEqual(result.backspace, 5)
                XCTAssertEqual(FunputComposer.output(of: result), "địa chỉ" + String(boundary))
                XCTAssertEqual(composer.buffer(), "")
            }
        }
    }

    func testMasterAndEnglishSwitchesGateExpansionWithoutUnloadingTable() {
        let composer = FunputComposer()
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        for (master, english) in [(false, true), (true, false), (true, true)] {
            composer.clear()
            composer.apply(.init(enabled: false, shortcutsEnabled: master, shortcutsInEnglish: english))
            for key in "vn".unicodeScalars { composer.process(key) }
            XCTAssertEqual(composer.process(" ").action, master && english ? UInt8(ACTION_SEND) : UInt8(ACTION_NONE))
        }
        composer.apply(.init(shortcutsInEnglish: false))
        for key in "vn".unicodeScalars { composer.process(key) }
        XCTAssertEqual(composer.process(" ").action, UInt8(ACTION_SEND))
    }

    func testBackspaceCaseMatchingAndModeChanges() {
        let composer = FunputComposer()
        composer.apply(.init(enabled: false))
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        for key in "VNx".unicodeScalars { composer.process(key) }
        composer.backspace()
        XCTAssertEqual(FunputComposer.output(of: composer.process(" ")), "VIỆT NAM ")
        composer.setShortcutSmartCase(false)
        for key in "VN".unicodeScalars { composer.process(key) }
        XCTAssertEqual(composer.process(" ").action, UInt8(ACTION_NONE))
        composer.process("v")
        composer.clear() // the controller clears the old session before a mode change
        composer.setEnabled(true)
        composer.setEnabled(false)
        composer.process("n")
        XCTAssertEqual(composer.process(" ").action, UInt8(ACTION_NONE))
    }

    func testLongTriggerAndExpansionAreNotTruncatedByTheCBridge() {
        let composer = FunputComposer()
        composer.apply(.init(enabled: false))
        let trigger = String(repeating: "v", count: 100)
        let expansion = String(repeating: "địa chỉ 😀 ", count: 100)
        composer.addShortcut(trigger: trigger, expansion: expansion)
        for key in trigger.unicodeScalars {
            XCTAssertTrue(composer.processText(key).output.isEmpty)
        }
        XCTAssertEqual(composer.buffer(), trigger)
        let (result, output) = composer.processText(" ")
        XCTAssertEqual(result.action, UInt8(ACTION_SEND))
        XCTAssertEqual(result.backspace, 100)
        XCTAssertEqual(output, expansion + " ")
    }

    func testDraftRowsAreExcludedFromEngineSync() {
        for row in [TextShortcut(trigger: "vn", expansion: ""),
                    TextShortcut(trigger: " ", expansion: "Việt Nam"),
                    TextShortcut(trigger: "vn", expansion: " \n")] {
            XCTAssertFalse(row.isComplete)
        }
        XCTAssertTrue(TextShortcut(trigger: "vn", expansion: "Việt Nam").isComplete)
    }

    func testNoTableLeavesEnglishCompletelyIdle() {
        let composer = FunputComposer()
        composer.apply(.init(enabled: false))
        for key in "address1 ".unicodeScalars {
            XCTAssertEqual(composer.processText(key).result.action, UInt8(ACTION_NONE))
            XCTAssertEqual(composer.buffer(), "")
        }
    }

    func testEnglishPreferencePersistsAndRoundTripsPortableConfig() throws {
        let suite = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let settings = AppSettings(defaults: defaults)
        XCTAssertTrue(settings.shortcutsInEnglish)
        settings.shortcutsInEnglish = false
        XCTAssertFalse(AppSettings(defaults: defaults).shortcutsInEnglish)
        XCTAssertFalse(ComposerConfiguration(settings: settings).shortcutsInEnglish)
        let exported = try settings.exportData()
        settings.shortcutsInEnglish = true
        try settings.importData(exported)
        XCTAssertFalse(settings.shortcutsInEnglish)
        let legacy = Data(#"{"schema":"app.funput.config","version":1,"preferences":{"shortcutsEnabled":true}}"#.utf8)
        // Older files leave the user's current preference intact.
        try settings.importData(legacy)
        XCTAssertFalse(settings.shortcutsInEnglish)
    }
}
