#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputCore

/// Hands an edit to the keyboard, which owns the store and applies it the next
/// time it appears. Writes one new file and nothing else, so it is safe while the
/// keyboard has the store open.
public enum PersonalDictionaryInbox {
    /// False when the word is longer than any the store holds, or the file could
    /// not be written.
    public static func post(_ kind: DictionaryEditKind, word: String, storeURL: URL) -> Bool {
        let codepoints = DictionaryFFI.codepoints(word)
        return DictionaryFFI.withPath(storeURL) { path, pathLength in
            codepoints.withUnsafeBufferPointer {
                funput_suggestion_post_edit(
                    path,
                    pathLength,
                    kind.code,
                    $0.baseAddress,
                    UInt($0.count)
                )
            }
        }
    }
}
#endif
