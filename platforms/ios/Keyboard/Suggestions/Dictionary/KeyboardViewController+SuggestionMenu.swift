import KeyboardRenderer
import PersonalDictionary
import PersonalSuggestions

extension KeyboardViewController {
    /// Offers the long-press menu on suggested words only where an edit lasts.
    ///
    /// Without Full Access the store lives in memory for this session alone, so a
    /// word blocked there would come back the next time the keyboard opens. The
    /// menu stays away rather than promise what it cannot keep.
    func configureSuggestionMenu(hasFullAccess: Bool) {
        keyboardView.onSuggestionEdit = hasFullAccess
            ? { [weak self] candidate, action in
                self?.personalSuggestionService.edit(candidate, action.dictionaryEdit)
            }
            : nil
    }
}

private extension KeyboardSuggestionAction {
    var dictionaryEdit: DictionaryEditKind {
        switch self {
        case .block: .block
        case .forget: .forget
        }
    }
}
