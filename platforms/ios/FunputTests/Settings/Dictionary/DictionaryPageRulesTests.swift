import Foundation
import FunputShared
import PersonalDictionary
import Testing
@testable import Funput

@MainActor
@Suite("Dictionary page rules")
struct DictionaryPageRulesTests {
    @Test("The add sheet flags a spaced or overlong word while it is typed")
    func draftProblems() {
        var draft = DictionaryWordDraft()
        #expect(!draft.canSave)
        draft.text = "  Funput  "
        #expect(draft.canSave)
        #expect(draft.suggestionPreview == "funput")
        draft.text = "hai từ"
        #expect(draft.problem == .spaced)
        draft.text = String(repeating: "a", count: DictionaryLimits.maxWordLength + 1)
        #expect(draft.problem == .tooLong)
    }

    @Test("Only a save that applied closes the sheet")
    func saveMessages() {
        #expect(DictionaryEditOutcome.applied.addMessage == nil)
        for outcome in [DictionaryEditOutcome.unchanged, .limitReached, .invalid] {
            #expect(outcome.addMessage != nil)
        }
    }

    @Test("A wipe the keyboard has not carried out is pending")
    func resetIsPendingUntilApplied() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        #expect(!model.personalSuggestionResetIsPending)
        model.requestPersonalSuggestionReset()
        #expect(model.personalSuggestionResetIsPending)
    }

    @Test("The suggestion card and the typing card split the switches between them")
    func smartInputGroups() {
        let suggestions = SmartInputToggle.suggestions.map(\.id)
        let typing = SmartInputToggle.typing.map(\.id)
        #expect(Set(suggestions).isDisjoint(with: typing))
        #expect(suggestions.count + typing.count == SmartInputToggle.all.count)
        #expect(SmartInputToggle.suggestions.contains { $0.keyPath == \.personalSuggestionsEnabled })
    }
}
