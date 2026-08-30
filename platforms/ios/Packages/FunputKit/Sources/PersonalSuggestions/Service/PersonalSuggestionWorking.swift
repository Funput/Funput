import Foundation

public struct PersonalSuggestionWorkerConfiguration: Equatable, Sendable {
    public let enabled: Bool
    public let hasFullAccess: Bool
    public let resetToken: UUID?

    public init(enabled: Bool, hasFullAccess: Bool, resetToken: UUID?) {
        self.enabled = enabled
        self.hasFullAccess = hasFullAccess
        self.resetToken = resetToken
    }
}

public protocol PersonalSuggestionWorking: AnyObject, Sendable {
    func configure(_ configuration: PersonalSuggestionWorkerConfiguration)
    func learn(_ token: String, after previous: String?)
    func query(_ request: PersonalSuggestionQueryRequest)
    func flush()
}

public typealias PersonalSuggestionResultHandler = @Sendable (
    PersonalSuggestionQueryRequest,
    [PersonalSuggestionCandidate]
) -> Void

public typealias PersonalSuggestionWorkerFactory = @MainActor (
    @escaping PersonalSuggestionResultHandler
) -> any PersonalSuggestionWorking
