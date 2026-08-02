import Foundation
import FunputShared

extension PersonalSuggestionWorker {
    static func prepareStoreURL() -> URL? {
        AppGroupDirectory.prepare(named: FunputAppGroup.personalSuggestionsDirectory)
    }
}
