/// What a long press on a suggested word can do to it.
///
/// The renderer only offers these; what they mean for the stored words is the
/// keyboard's business.
public enum KeyboardSuggestionAction: CaseIterable, Sendable {
    /// Never offer this word again.
    case block
    /// Drop what was learned about it; typing it again relearns it.
    case forget

    var title: String {
        switch self {
        case .block: "Không gợi ý từ này"
        case .forget: "Quên từ này"
        }
    }

    var symbolName: String {
        switch self {
        case .block: "nosign"
        case .forget: "trash"
        }
    }

    var isDestructive: Bool { self == .forget }
}
