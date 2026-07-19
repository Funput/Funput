import Foundation

enum PersonalSuggestionCasing {
    private static let vietnamese = Locale(identifier: "vi_VN")

    static func apply(_ candidate: String, matching prefix: String) -> String {
        let letters = prefix.filter { $0.isLetter }
        guard !letters.isEmpty else { return candidate }
        if letters == letters.uppercased(with: vietnamese) {
            return candidate.uppercased(with: vietnamese)
        }
        guard let first = prefix.first,
              String(first) == String(first).uppercased(with: vietnamese) else {
            return candidate
        }
        return candidate.prefix(1).uppercased(with: vietnamese) + candidate.dropFirst()
    }
}
