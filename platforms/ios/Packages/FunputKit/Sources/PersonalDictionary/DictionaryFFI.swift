#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputCore

/// The C side of the dictionary: codes, text marshalling and list decoding.
///
/// `package` so the keyboard's engine in `PersonalSuggestions` speaks the same
/// codes without the app ever seeing them.
package enum DictionaryFFI {
    package static func codepoints(_ text: String) -> [UInt32] {
        text.unicodeScalars.map(\.value)
    }

    package static func withPath<Result>(
        _ url: URL,
        _ body: (UnsafePointer<UInt8>?, UInt) -> Result
    ) -> Result {
        Array(url.path.utf8).withUnsafeBufferPointer { body($0.baseAddress, UInt($0.count)) }
    }

    /// Reads every entry out of a list handle, then frees it.
    static func drain(_ list: OpaquePointer?) -> [DictionaryWord] {
        guard let list else { return [] }
        defer { funput_suggestion_dictionary_list_free(list) }
        let count = Int(funput_suggestion_dictionary_list_len(list))
        var entry = FunputDictionaryEntry()
        return (0..<count).compactMap { index in
            guard funput_suggestion_dictionary_list_entry(list, UInt(index), &entry) else {
                return nil
            }
            return word(entry)
        }
    }

    private static func word(_ entry: FunputDictionaryEntry) -> DictionaryWord? {
        var copy = entry
        let count = min(Int(entry.count), Int(SUGGESTION_CHARS_CAP))
        let text = withUnsafePointer(to: &copy.chars) { tuple in
            tuple.withMemoryRebound(to: UInt32.self, capacity: Int(SUGGESTION_CHARS_CAP)) {
                string($0, count: count)
            }
        }
        guard let text, !text.isEmpty else { return nil }
        return DictionaryWord(
            text: text,
            uses: Int(entry.uses),
            lastUsed: entry.last_used,
            origin: DictionaryOrigin(code: entry.origin)
        )
    }

    private static func string(_ codepoints: UnsafePointer<UInt32>, count: Int) -> String? {
        var scalars = String.UnicodeScalarView()
        for index in 0..<count {
            guard let scalar = Unicode.Scalar(codepoints[index]) else { return nil }
            scalars.append(scalar)
        }
        return String(scalars)
    }
}

extension DictionaryEditKind {
    package var code: UInt32 {
        switch self {
        case .add: UInt32(FUNPUT_DICTIONARY_EDIT_ADD)
        case .forget: UInt32(FUNPUT_DICTIONARY_EDIT_FORGET)
        case .block: UInt32(FUNPUT_DICTIONARY_EDIT_BLOCK)
        case .unblock: UInt32(FUNPUT_DICTIONARY_EDIT_UNBLOCK)
        }
    }
}

extension DictionaryEditOutcome {
    /// An unknown code is treated as a refusal rather than guessed at.
    package init(code: UInt32) {
        switch code {
        case UInt32(FUNPUT_DICTIONARY_APPLIED): self = .applied
        case UInt32(FUNPUT_DICTIONARY_UNCHANGED): self = .unchanged
        case UInt32(FUNPUT_DICTIONARY_LIMIT_REACHED): self = .limitReached
        default: self = .invalid
        }
    }
}

extension DictionaryOrigin {
    init(code: UInt32) {
        switch code {
        case UInt32(FUNPUT_DICTIONARY_ORIGIN_ADDED): self = .added
        case UInt32(FUNPUT_DICTIONARY_ORIGIN_BLOCKED): self = .blocked
        default: self = .learned
        }
    }
}

extension DictionarySort {
    var code: UInt32 {
        switch self {
        case .mostUsed: UInt32(FUNPUT_DICTIONARY_SORT_MOST_USED)
        case .recent: UInt32(FUNPUT_DICTIONARY_SORT_RECENT)
        case .alphabetical: UInt32(FUNPUT_DICTIONARY_SORT_ALPHABETICAL)
        }
    }
}
#endif
