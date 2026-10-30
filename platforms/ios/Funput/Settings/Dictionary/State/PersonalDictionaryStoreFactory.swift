import Foundation
import FunputShared
import PersonalDictionary

/// The store the dictionary page reads: the keyboard's own, or a seeded one for
/// previews and UI tests so they never touch the user's words.
@MainActor
enum PersonalDictionaryStoreFactory {
    static func make() -> any PersonalDictionaryStoring {
#if DEBUG
        let environment = ProcessInfo.processInfo.environment
        if environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1" { return DictionaryPreviewStore() }
        switch environment["FUNPUT_DICTIONARY_TEST_SEED"] {
        case "samples": return DictionaryPreviewStore()
        case "empty": return DictionaryPreviewStore(words: [])
        default: break
        }
#endif
        return live()
    }

    private static func live() -> any PersonalDictionaryStoring {
        // Not `AppGroupDirectory.prepare`: the app only reads, and the keyboard
        // creates the directory with its protection class when it first opens it.
        let directory = AppGroupDirectory.containerURL()?
            .appendingPathComponent(FunputAppGroup.personalSuggestionsDirectory, isDirectory: true)
        guard let directory else { return UnavailableDictionaryStore() }
        return PersonalDictionaryStore(storeURL: directory)
    }
}

/// What the page reads when the App Group itself is missing: an honest error.
private struct UnavailableDictionaryStore: PersonalDictionaryStoring {
    func load(_ query: DictionaryQuery) async throws -> DictionaryPage {
        throw PersonalDictionaryError.unreadable
    }

    func apply(_ kind: DictionaryEditKind, word: String) async throws -> DictionaryEditOutcome {
        throw PersonalDictionaryError.unwritable
    }
}
