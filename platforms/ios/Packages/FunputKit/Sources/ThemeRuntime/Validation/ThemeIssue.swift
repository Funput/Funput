/// A problem found while validating an authored theme.
///
/// Issues are advisory: they let an authoring UI warn the user. Resolution
/// still clamps metrics, so an issue never blocks a theme from rendering.
public struct ThemeIssue: Hashable, Sendable {
    public enum Kind: Hashable, Sendable {
        case lowContrast
    }

    public let kind: Kind
    public let message: String

    public init(kind: Kind, message: String) {
        self.kind = kind
        self.message = message
    }
}
