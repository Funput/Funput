#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputCore

/// A read-only view of the store the keyboard owns, as the app sees it.
///
/// Opening writes nothing, and includes the edits still waiting in the inbox, so a
/// listing shows what the store is about to be. Edits made here change only this
/// view: `preview` asks the engine whether an edit would apply, under exactly the
/// rules the keyboard will apply it by, and ``PersonalDictionaryInbox`` makes it
/// real. One owner at a time, like every engine handle.
public final class PersonalDictionarySnapshot: @unchecked Sendable {
    private let handle: OpaquePointer

    public static func open(storeURL: URL) -> PersonalDictionarySnapshot? {
        let handle = DictionaryFFI.withPath(storeURL) {
            funput_suggestion_engine_open_snapshot($0, $1)
        }
        return handle.map(PersonalDictionarySnapshot.init(handle:))
    }

    private init(handle: OpaquePointer) {
        self.handle = handle
    }

    deinit {
        funput_suggestion_engine_free(handle)
    }

    public func words(in query: DictionaryQuery) -> [DictionaryWord] {
        switch query.filter {
        case .learned: listed(FUNPUT_DICTIONARY_FILTER_LEARNED, query)
        case .added: listed(FUNPUT_DICTIONARY_FILTER_ADDED, query)
        case .blocked: blocked(matching: query.search)
        }
    }

    public func count(of filter: DictionaryFilter) -> Int {
        words(in: DictionaryQuery(filter: filter)).count
    }

    /// Applies `kind` to this view only, and says what it did.
    public func preview(_ kind: DictionaryEditKind, word: String) -> DictionaryEditOutcome {
        let codepoints = DictionaryFFI.codepoints(word)
        let code = codepoints.withUnsafeBufferPointer {
            funput_suggestion_apply_edit(handle, kind.code, $0.baseAddress, UInt($0.count))
        }
        return DictionaryEditOutcome(code: code)
    }

    private func listed(_ filter: Int32, _ query: DictionaryQuery) -> [DictionaryWord] {
        let search = DictionaryFFI.codepoints(query.search)
        let list = search.withUnsafeBufferPointer {
            funput_suggestion_dictionary_list(
                handle,
                UInt32(filter),
                query.sort.code,
                $0.baseAddress,
                UInt($0.count)
            )
        }
        return DictionaryFFI.drain(list)
    }

    /// The engine lists blocked words without a search, since there are few of them;
    /// the search is the same mark-blind match the other lists use.
    private func blocked(matching search: String) -> [DictionaryWord] {
        let words = DictionaryFFI.drain(funput_suggestion_blocked_list(handle))
        let needle = DictionarySearchKey(search)
        return needle.isEmpty ? words : words.filter { DictionarySearchKey($0.text).contains(needle) }
    }
}
#endif

/// A word with its case and Vietnamese marks folded away, `đ` included, so `duoc`
/// finds `được` as it does in the engine's own lists.
public struct DictionarySearchKey: Sendable {
    private let folded: String

    public init(_ text: String) {
        folded = text
            .folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
            .replacingOccurrences(of: "đ", with: "d")
    }

    public var isEmpty: Bool { folded.isEmpty }

    public func contains(_ other: DictionarySearchKey) -> Bool {
        folded.contains(other.folded)
    }
}
