import Foundation
import KeyboardInput
import PersonalDictionary

public struct PersonalSuggestionWorkerConfiguration: Equatable, Sendable {
    public let enabled: Bool
    public let hasFullAccess: Bool
    public let resetToken: UUID?
    /// What the shipped Vietnamese data may say, so a worker created later starts with it.
    public let seedLanguage: SuggestionSeedLanguage

    public init(
        enabled: Bool,
        hasFullAccess: Bool,
        resetToken: UUID?,
        seedLanguage: SuggestionSeedLanguage = .off
    ) {
        self.enabled = enabled
        self.hasFullAccess = hasFullAccess
        self.resetToken = resetToken
        self.seedLanguage = seedLanguage
    }
}

public protocol PersonalSuggestionWorking: AnyObject, Sendable {
    func configure(_ configuration: PersonalSuggestionWorkerConfiguration)
    func learn(_ token: String, after previous: String?)
    func query(_ request: PersonalSuggestionQueryRequest)
    func flush()
    /// A change of language or tone style between configurations. Takes effect before
    /// any query submitted after it.
    func setSeedLanguage(_ language: SuggestionSeedLanguage)
    /// Applies a dictionary edit to the store and makes it durable.
    func edit(_ kind: DictionaryEditKind, word: String)
}

public typealias PersonalSuggestionResultHandler = @Sendable (
    PersonalSuggestionQueryRequest,
    [PersonalSuggestionCandidate]
) -> Void

public typealias PersonalSuggestionWorkerFactory = @MainActor (
    @escaping PersonalSuggestionResultHandler
) -> any PersonalSuggestionWorking
