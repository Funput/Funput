public struct KeyboardClipboardPaste: Sendable {
    public let text: String
    public let changeCount: Int?

    public init(text: String, changeCount: Int?) {
        self.text = text
        self.changeCount = changeCount
    }
}
