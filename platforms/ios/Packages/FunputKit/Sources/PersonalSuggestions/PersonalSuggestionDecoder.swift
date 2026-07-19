#if os(iOS) && canImport(FunputCore)
import FunputCore

enum PersonalSuggestionDecoder {
    static func candidates(_ result: FunputSuggestionResult) -> [PersonalSuggestionCandidate] {
        var copy = result
        let count = min(Int(result.count), Int(SUGGESTION_CAP))
        return withUnsafePointer(to: &copy.candidates) { tuple in
            tuple.withMemoryRebound(
                to: FunputSuggestionCandidate.self,
                capacity: Int(SUGGESTION_CAP)
            ) { buffer in
                (0..<count).compactMap { candidate(buffer[$0]) }
            }
        }
    }

    private static func candidate(
        _ candidate: FunputSuggestionCandidate
    ) -> PersonalSuggestionCandidate? {
        var copy = candidate
        let count = min(Int(candidate.count), Int(SUGGESTION_CHARS_CAP))
        let text = withUnsafePointer(to: &copy.chars) { tuple in
            tuple.withMemoryRebound(to: UInt32.self, capacity: Int(SUGGESTION_CHARS_CAP)) {
                string($0, count: count)
            }
        }
        return text.isEmpty ? nil : PersonalSuggestionCandidate(text: text)
    }

    private static func string(_ codepoints: UnsafePointer<UInt32>, count: Int) -> String {
        var scalars = String.UnicodeScalarView()
        for index in 0..<count {
            guard let scalar = Unicode.Scalar(codepoints[index]) else { return "" }
            scalars.append(scalar)
        }
        return String(scalars)
    }
}
#endif
