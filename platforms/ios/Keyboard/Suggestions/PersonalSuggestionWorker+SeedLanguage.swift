import Foundation
import KeyboardInput
import PersonalSuggestions

extension PersonalSuggestionWorker {
    /// Takes effect before any query submitted after it. `drainQueries` can take a new
    /// query in the same pass as the one before it, ahead of anything enqueued in
    /// between, so a posted block alone could let that query run on the old setting.
    /// The value waits in `pendingSeed`, and every drain applies it before it queries;
    /// the block covers a change with no query behind it.
    func setSeedLanguage(_ language: SuggestionSeedLanguage) {
        pendingSeed.submit(language)
        queue.async { [weak self] in self?.applyPendingSeed() }
    }

    /// Queue only.
    func applyPendingSeed() {
        guard let language = pendingSeed.take() else { return }
        seedLanguage = language
        engine?.setSeedLanguage(language)
    }
}
