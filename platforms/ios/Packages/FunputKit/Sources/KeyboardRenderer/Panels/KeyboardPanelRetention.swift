/// A panel the keyboard can show in place of its letters.
public enum KeyboardPanel: CaseIterable, Hashable, Sendable {
    case emoji
    case kaomoji
    case clipboard
}

/// Which panels stay alive for a given surface on screen.
///
/// A panel is cheap to rebuild and expensive to keep: the emoji grid alone holds its
/// catalog, its cells' bitmaps and their layers, and an extension past its memory
/// limit is killed without warning. So the rule keeps only what the visible surface
/// can lead straight back to. Emoji and kaomoji link to each other, so while either
/// shows the other is kept for the way back; anything else is let go.
public enum KeyboardPanelRetention {
    /// The panels to keep while `showing` is on screen; nil means the letters.
    public static func retained(whileShowing showing: KeyboardPanel?) -> Set<KeyboardPanel> {
        switch showing {
        case .emoji?, .kaomoji?: [.emoji, .kaomoji]
        case .clipboard?: [.clipboard]
        case nil: []
        }
    }
}
