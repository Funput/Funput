public struct KeyboardPostCommitEffects: Equatable, Sendable {
    public let presentationChanged: Bool
    public let suggestionsChanged: Bool

    public init(
        presentationChanged: Bool = false,
        suggestionsChanged: Bool = false
    ) {
        self.presentationChanged = presentationChanged
        self.suggestionsChanged = suggestionsChanged
    }

    public static let none = Self()
}
