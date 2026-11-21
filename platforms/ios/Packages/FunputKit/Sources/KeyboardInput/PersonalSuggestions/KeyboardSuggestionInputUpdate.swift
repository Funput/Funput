/// What the token tracker hands the suggestion service after an edit.
public struct KeyboardSuggestionInputUpdate: Equatable, Sendable {
    public let prefix: String
    public let completedToken: String?
    /// The word `prefix` is being typed after, when one can be vouched for.
    public let context: String?
    /// The word `context` stands after, when the two stand in a row.
    public let older: String?
    /// Whether `context` was read from the document rather than typed through Funput.
    public let borrowed: Bool

    public init(
        prefix: String,
        completedToken: String?,
        context: String? = nil,
        older: String? = nil,
        borrowed: Bool = false
    ) {
        self.prefix = prefix
        self.completedToken = completedToken
        self.context = context
        self.older = context == nil ? nil : older
        self.borrowed = context != nil && borrowed
    }

    /// The words before `prefix` as the engine reads them: oldest first, space separated.
    public var queryContext: String? {
        guard let context else { return nil }
        guard let older else { return context }
        return older + " " + context
    }

    /// The word a completed token is learned as following. Never a borrowed one: the
    /// store learns what was typed through Funput, not what a document already held.
    public var learnContext: String? { borrowed ? nil : context }

    public static let empty = KeyboardSuggestionInputUpdate(prefix: "", completedToken: nil)
}
