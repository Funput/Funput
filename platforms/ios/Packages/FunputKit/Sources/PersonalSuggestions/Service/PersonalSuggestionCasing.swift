import Foundation

enum PersonalSuggestionCasing {
    private static let vietnamese = Locale(identifier: "vi_VN")

    /// A prediction has no prefix to take its shape from, so `capitalized` — the
    /// keyboard's own Shift state — is the only thing left to ask.
    static func apply(
        _ candidate: String,
        matching prefix: String,
        capitalized: Bool = false
    ) -> String {
        let letters = prefix.filter(\.isLetter)
        guard !letters.isEmpty else {
            guard capitalized else { return candidate }
            return candidate.prefix(1).uppercased(with: vietnamese) + candidate.dropFirst()
        }
        if letters == letters.uppercased(with: vietnamese) {
            return candidate.uppercased(with: vietnamese)
        }
        guard let first = prefix.first,
              String(first) == String(first).uppercased(with: vietnamese)
        else { return candidate }
        return candidate.prefix(1).uppercased(with: vietnamese)
            + candidate.dropFirst()
    }
}
