import FunputShared
import PersonalSuggestions
import UIKit

extension KeyboardViewController {
    /// Hand typo correction the words the user has taken corrections back on, and
    /// store each one it adds.
    ///
    /// Stored only with Full Access, the same line the personal lexicon draws:
    /// without it the keyboard cannot write to the shared container, and the list
    /// lasts as long as this keyboard does. A lexicon reset clears it too — the
    /// store reads a list written under another reset token as empty.
    func restoreKeptWords(hasFullAccess: Bool) {
        let token = configuration.personalSuggestionResetToken
        guard hasFullAccess else {
            correctionWords.onKeep = nil
            return
        }
        let store = TypoKeptWordsStore()
        correctionWords.restoreKept(store.load(resetToken: token))
        correctionWords.onKeep = { words in
            store.save(words, resetToken: token)
        }
    }
}
