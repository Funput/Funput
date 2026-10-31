#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing

/// The coordinator is the one writer of the composer's Vietnamese switch, so it is the
/// one place the shipped suggestion data hears about the language and the tone style.
@MainActor
struct KeyboardInputSeedLanguageTests {
    private final class Reports {
        var values: [SuggestionSeedLanguage] = []
    }

    private func coordinator(reporting reports: Reports) -> KeyboardInputCoordinator {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        coordinator.onSeedLanguageChange = { reports.values.append($0) }
        return coordinator
    }

    @Test("Settings report the tone style the composer writes")
    func settingsReportToneStyle() {
        let reports = Reports()
        let coordinator = coordinator(reporting: reports)

        coordinator.apply(FunputConfiguration(inputMethod: .telex, toneStyle: .modern))
        coordinator.apply(FunputConfiguration(inputMethod: .telex, toneStyle: .traditional))

        #expect(reports.values == [.vietnamese(.modern), .vietnamese(.traditional)])
        #expect(coordinator.seedLanguage == .vietnamese(.traditional))
    }

    @Test("The language key turns it off and back on")
    func languageKeyToggles() {
        let reports = Reports()
        let coordinator = coordinator(reporting: reports)
        coordinator.apply(FunputConfiguration(inputMethod: .telex))

        coordinator.toggleLanguage()
        coordinator.toggleLanguage()

        #expect(reports.values == [.vietnamese(.modern), .off, .vietnamese(.modern)])
    }

    @Test("An English preference starts off")
    func englishPreferenceIsOff() {
        let reports = Reports()
        let coordinator = coordinator(reporting: reports)

        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english))

        #expect(reports.values == [.off])
    }

    @Test("A field that does not compose turns it off")
    func nonComposingFieldIsOff() {
        let reports = Reports()
        let coordinator = coordinator(reporting: reports)
        coordinator.apply(FunputConfiguration(inputMethod: .telex))

        coordinator.updateContext(inputContext(editorMode: .email, enterAction: .done))
        coordinator.updateContext(inputContext(editorMode: .text, enterAction: .newLine))

        #expect(reports.values == [.vietnamese(.modern), .off, .vietnamese(.modern)])
    }

    @Test("Reapplying the same settings reports nothing new")
    func repeatsAreQuiet() {
        let reports = Reports()
        let coordinator = coordinator(reporting: reports)

        coordinator.apply(FunputConfiguration(inputMethod: .telex))
        coordinator.apply(FunputConfiguration(inputMethod: .telex))

        #expect(reports.values == [.vietnamese(.modern)])
    }

    @Test("Nothing is configured yet, so nothing is on")
    func unconfiguredIsOff() {
        #expect(KeyboardInputCoordinator(inputMethod: .telex).seedLanguage == .off)
    }
}
#endif
