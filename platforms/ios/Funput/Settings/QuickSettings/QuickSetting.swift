import FunputShared

/// A switch the keyboard tab offers as a one-tap tile.
///
/// The six chosen are the ones people flip back and forth to taste — feedback, capitals,
/// suggestions, the number row — rather than set once at setup. Each stays on its own
/// page too; the tile is a shortcut, not a second home.
enum QuickSetting: CaseIterable, Identifiable {
    case haptics
    case sound
    case autoCapitalize
    case suggestions
    case numberRow
    case keyPreviews

    var id: Self { self }

    var title: String {
        switch self {
        case .haptics: "Rung"
        case .sound: "Âm thanh"
        case .autoCapitalize: "Viết hoa"
        case .suggestions: "Gợi ý từ"
        case .numberRow: "Hàng số"
        case .keyPreviews: "Xem trước"
        }
    }

    /// The full name, for VoiceOver, where the tile's short label would be ambiguous.
    var accessibilityTitle: String {
        switch self {
        case .haptics: "Rung khi gõ"
        case .sound: "Âm thanh khi gõ"
        case .autoCapitalize: "Tự viết hoa"
        case .suggestions: "Hiện gợi ý từ"
        case .numberRow: "Hàng phím số"
        case .keyPreviews: "Xem trước phím"
        }
    }

    var systemImage: String {
        switch self {
        case .haptics: "iphone.radiowaves.left.and.right"
        case .sound: "speaker.wave.2"
        case .autoCapitalize: "textformat.size.larger"
        case .suggestions: "text.badge.star"
        case .numberRow: "textformat.123"
        case .keyPreviews: "character.magnify"
        }
    }

    var keyPath: WritableKeyPath<FunputConfiguration, Bool> {
        switch self {
        case .haptics: \.isHapticFeedbackEnabled
        case .sound: \.isKeySoundEnabled
        case .autoCapitalize: \.autoCapitalize
        case .suggestions: \.personalSuggestionsEnabled
        case .numberRow: \.showsNumberRow
        case .keyPreviews: \.showsKeyPreviews
        }
    }

    /// Haptics and sound do nothing without Full Access; switching them on asks for it.
    var fullAccessFeature: FullAccessFeature? {
        switch self {
        case .haptics: .haptics
        case .sound: .sound
        default: nil
        }
    }
}
