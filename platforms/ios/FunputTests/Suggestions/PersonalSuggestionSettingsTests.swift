import Foundation
import FunputShared
import Testing
@testable import Funput

@MainActor
@Suite("Personal suggestion settings")
struct PersonalSuggestionSettingsTests {
    @Test("Dedicated reset creates a new command token")
    func requestsReset() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)
        #expect(model.configuration.personalSuggestionResetToken == nil)
        model.requestPersonalSuggestionReset()
        #expect(model.configuration.personalSuggestionResetToken != nil)
        #expect(store.configuration.personalSuggestionResetToken == model.configuration.personalSuggestionResetToken)
    }

    @Test("Resetting preferences does not replay lexicon deletion")
    func settingsResetPreservesToken() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = false
        configuration.personalSuggestionResetToken = UUID()
        let token = configuration.personalSuggestionResetToken
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        model.reset()
        #expect(model.configuration.personalSuggestionsEnabled)
        #expect(model.configuration.personalSuggestionResetToken == token)
    }
}
