import XCTest
@testable import Funput

/// The extra onsets `z`, `f`, `w`, `j`: the letter set, the setting, the config file,
/// and typing through the real engine.
@MainActor
final class ExtraOnsetsTests: XCTestCase {
    private func makeSettings() -> (AppSettings, String) {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        return (AppSettings(defaults: UserDefaults(suiteName: suiteName)!), suiteName)
    }

    /// The app text after typing `keys`, rebuilt from the engine's instructions.
    private func typed(_ keys: String, method: InputMethod = .telex, letters: ExtraOnsetLetters) -> String {
        let composer = FunputComposer()
        composer.apply(.init(inputMethod: method, toneStyle: .traditional, extraOnsets: letters))
        var text = ""
        for key in keys.unicodeScalars {
            let result = composer.process(key)
            if result.action == UInt8(ACTION_NONE) {
                text.unicodeScalars.append(key)
            } else {
                text.removeLast(Int(result.backspace))
                text += FunputComposer.output(of: result)
            }
        }
        return text
    }

    func testLettersAreTheEngineWireBits() {
        XCTAssertEqual(ExtraOnsetLetters.f.rawValue, UInt8(ONSET_F))
        XCTAssertEqual(ExtraOnsetLetters.j.rawValue, UInt8(ONSET_J))
        XCTAssertEqual(ExtraOnsetLetters.w.rawValue, UInt8(ONSET_W))
        XCTAssertEqual(ExtraOnsetLetters.z.rawValue, UInt8(ONSET_Z))
        XCTAssertEqual(ExtraOnsetLetters.letters.map(\.symbol), ["z", "f", "w", "j"])
    }

    func testConfigValueSpellsTheLettersAndIgnoresStrangers() {
        XCTAssertEqual(ExtraOnsetLetters([.j, .z]).configValue, "zj")
        XCTAssertEqual(ExtraOnsetLetters.all.configValue, "zfwj")
        XCTAssertEqual(ExtraOnsetLetters(configValue: "JZ"), [.z, .j])
        XCTAssertEqual(ExtraOnsetLetters(configValue: "zxq"), [.z])
        XCTAssertEqual(ExtraOnsetLetters(configValue: ""), [])
    }

    func testOffByDefaultAndSnapshotCarriesTheChoice() {
        let (settings, suite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        XCTAssertEqual(settings.extraOnsets, [])
        XCTAssertEqual(ComposerConfiguration(settings: settings).extraOnsets, [])

        settings.extraOnsets = [.z, .w]
        XCTAssertEqual(ComposerConfiguration(settings: settings).extraOnsets, [.z, .w])
        let reloaded = AppSettings(defaults: UserDefaults(suiteName: suite)!)
        XCTAssertEqual(reloaded.extraOnsets, [.z, .w], "persists across launches")
    }

    func testChoiceSurvivesExportAndImport() throws {
        let (source, sourceSuite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: sourceSuite) }
        source.extraOnsets = [.f, .j]
        let data = try source.exportData()

        let (destination, destinationSuite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: destinationSuite) }
        try destination.importData(data)
        XCTAssertEqual(destination.extraOnsets, [.f, .j])
    }

    func testAFileWithoutTheFieldLeavesTheLocalChoiceAlone() throws {
        let (settings, suite) = makeSettings()
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        settings.extraOnsets = [.z]
        let legacy = """
        { "schema": "app.funput.config", "version": 1,
          "preferences": { "inputMethod": "vni" } }
        """
        try settings.importData(Data(legacy.utf8))
        XCTAssertEqual(settings.extraOnsets, [.z])
    }

    func testOnlyTheChosenLettersOpenASyllable() {
        XCTAssertEqual(typed("zoo fair ", letters: [.z]), "zô fair ")
        XCTAssertEqual(typed("zoo fair ", letters: [.z, .f]), "zô fải ")
        XCTAssertEqual(typed("jowf was food ", letters: .all), "jờ wá food ")
        XCTAssertEqual(typed("zoo jowf fair ", letters: []), "zoo jowf fair ")
        XCTAssertEqual(typed("zo6 jo72 ", method: .vni, letters: .all), "zô jờ ")
    }

    func testFullTelexKeepsItsLeadingW() {
        XCTAssertEqual(typed("wa wwas ", method: .telexAdvanced, letters: .all), "ưa wá ")
    }
}
