import FunputEngine

/// What the shipped Vietnamese suggestion data may say: nothing, or predictions
/// spelled in the tone style the composer writes.
public enum SuggestionSeedLanguage: Equatable, Sendable {
    /// English, a field that does not compose (password, URL), or not yet configured.
    case off
    case vietnamese(FunputToneStyle)
}
