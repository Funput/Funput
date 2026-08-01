extension KeyboardDocumentEvent {
    /// Whether the callback can be an acknowledgement of a mutation Funput just authored.
    ///
    /// A host acknowledges an insertion through both callbacks: the text changes and the caret
    /// moves. Under fast typing those echoes arrive after the next keystroke, so treating a
    /// selection echo as an external edit clears the composer mid-word and the following
    /// modifier lands as a literal. Only `.activated` is unconditionally external — it means a
    /// different text input took focus.
    var acknowledgesAuthoredEcho: Bool {
        switch self {
        case .textChanged, .selectionChanged: true
        case .activated: false
        }
    }
}
