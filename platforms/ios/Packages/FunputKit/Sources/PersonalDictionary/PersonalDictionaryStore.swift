import Foundation

/// What the dictionary page reads and changes. The live store talks to the engine;
/// tests and previews supply their own.
public protocol PersonalDictionaryStoring: Sendable {
    func load(_ query: DictionaryQuery) async throws -> DictionaryPage
    func apply(_ kind: DictionaryEditKind, word: String) async throws -> DictionaryEditOutcome
}

public enum PersonalDictionaryError: LocalizedError, Equatable {
    case unreadable
    case unwritable

    public var errorDescription: String? {
        switch self {
        case .unreadable: "Không đọc được từ điển. Hãy thử lại sau."
        case .unwritable: "Không lưu được thay đổi. Hãy thử lại sau."
        }
    }
}

#if os(iOS) && canImport(FunputCore)
/// The store the keyboard owns, read and changed from the app.
///
/// Every load reopens the snapshot, because the keyboard may have learned or
/// drained edits since. An edit is previewed on the open snapshot first, so a word
/// the keyboard would refuse is refused here with the same outcome, and only an
/// edit that applies is posted to the inbox.
public actor PersonalDictionaryStore: PersonalDictionaryStoring {
    private let storeURL: URL
    private var snapshot: PersonalDictionarySnapshot?

    public init(storeURL: URL) {
        self.storeURL = storeURL
    }

    public func load(_ query: DictionaryQuery) throws -> DictionaryPage {
        guard let snapshot = PersonalDictionarySnapshot.open(storeURL: storeURL) else {
            throw PersonalDictionaryError.unreadable
        }
        self.snapshot = snapshot
        let counts = Dictionary(
            uniqueKeysWithValues: DictionaryFilter.allCases.map { ($0, snapshot.count(of: $0)) }
        )
        return DictionaryPage(words: snapshot.words(in: query), counts: counts)
    }

    public func apply(_ kind: DictionaryEditKind, word: String) throws -> DictionaryEditOutcome {
        guard let snapshot = snapshot ?? PersonalDictionarySnapshot.open(storeURL: storeURL) else {
            throw PersonalDictionaryError.unreadable
        }
        self.snapshot = snapshot
        let outcome = snapshot.preview(kind, word: word)
        guard outcome == .applied else { return outcome }
        guard PersonalDictionaryInbox.post(kind, word: word, storeURL: storeURL) else {
            // The view now holds an edit the store never will; the next load drops it.
            self.snapshot = nil
            throw PersonalDictionaryError.unwritable
        }
        return outcome
    }
}
#endif
