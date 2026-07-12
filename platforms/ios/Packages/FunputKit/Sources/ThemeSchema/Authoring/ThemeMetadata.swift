/// Human-facing descriptive fields of a theme.
public struct ThemeMetadata: Codable, Hashable, Sendable {
    public var name: String
    public var author: String

    public init(name: String, author: String) {
        self.name = name
        self.author = author
    }
}
