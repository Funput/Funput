import Foundation
import KeyboardLayout

/// The shape a suggestion is shown in.
///
/// Learned words are stored lowercase and dictionary words keep their own case, so this
/// is the only thing standing between the store and what the user reads.
enum SuggestionCaseStyle {
    case upper
    case title
    /// Whatever the store holds, which is how `ip` keeps offering `iPhone`.
    case inherit

    private static let vietnamese = Locale(identifier: "vi_VN")

    /// Shift outranks the prefix. Pressing it over an already typed `vi` is a request
    /// for capitals, and accepting the suggestion rewrites those letters anyway — the
    /// accept path deletes the prefix before it inserts. With Shift down the prefix is
    /// the only evidence, and it is evidence the user committed.
    static func resolve(prefix: String, shift: ShiftState) -> SuggestionCaseStyle {
        switch shift {
        case .capsLocked: return .upper
        case .uppercase: return .title
        case .lowercase: break
        }
        // Letters only, so `1v` is read by its `v` rather than by its digit.
        let letters = prefix.filter(\.isLetter)
        guard let first = letters.first, isUpper(first) else { return .inherit }
        let rest = letters.dropFirst()
        // Shouting takes two letters to say. A lone capital is how every sentence
        // starts, which is why the shortcut rule reads `V` as Title too.
        if !rest.isEmpty, rest.allSatisfy(isUpper) { return .upper }
        // A prefix the user cased deliberately — `VNa`, `iOS` — is left to speak for
        // itself, the way `classify_case` returns nothing for it.
        return rest.allSatisfy { !isUpper($0) } ? .title : .inherit
    }

    private static func isUpper(_ character: Character) -> Bool {
        String(character) == String(character).uppercased(with: Self.vietnamese)
    }

    func apply(to candidate: String) -> String {
        switch self {
        case .upper:
            return candidate.uppercased(with: Self.vietnamese)
        case .title:
            return candidate.prefix(1).uppercased(with: Self.vietnamese) + candidate.dropFirst()
        case .inherit:
            return candidate
        }
    }
}
