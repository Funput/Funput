#if canImport(UIKit)
import KeyboardLayout
import UIKit

enum EmojiSearchState {
    case browsing
    case editing
    case showingResults
}

extension EmojiKeyboardView {
    func beginSearch() {
        searchState = .editing
        updateSearchUI()
    }

    func clearSearch() {
        searchComposer.reset()
        updateSearchUI()
    }

    func resetSearch() {
        searchState = .browsing
        searchComposer.reset()
        searchShiftState = .lowercase
        updateSearchUI()
    }

    func handleSearchKey(_ event: KeyboardKeyEvent) {
        guard event.phase == .released || event.phase == .repeated else { return }
        let key = event.key
        switch key.role {
        case .character:
            searchComposer.apply(.text(searchCharacter(for: key)))
        case .vniModifier, .punctuation:
            searchComposer.apply(.text(key.label))
        case .space:
            searchComposer.apply(.space)
        case .backspace:
            searchComposer.apply(.deleteBackward)
        case .shift:
            searchShiftState = searchShiftState == .lowercase ? .uppercase : .lowercase
        case .enter:
            searchState = .showingResults
        case .emoji:
            resetSearch()
            return
        default:
            return
        }
        updateSearchUI()
    }

    func updateSearchUI() {
        let browsing = searchState == .browsing
        collectionView.isHidden = !browsing
        bottomBar.isHidden = !browsing
        searchResults.isHidden = browsing
        searchKeyboard.isHidden = searchState != .editing
        applyPresentation()
        setNeedsLayout()
    }

    func updateSearchResults() {
        guard searchState != .browsing else { return }
        let items = searchIndex.search(searchQuery)
        let message = searchQuery.isEmpty
            ? "Nhập tên biểu tượng"
            : "Không tìm thấy biểu tượng"
        searchResults.apply(
            items: items,
            emptyMessage: message,
            expanded: searchState == .showingResults,
            color: theme.secondaryLabel.uiColor(for: traitCollection)
        )
    }

    func applySearchKeyboardPresentation() {
        var value = presentation
        value.layout = PanelSearchKeyboardLayouts.letters(
            presentation.layout.inputMethod,
            spaceLabel: "Tìm emoji"
        )
        value.shiftState = searchShiftState
        value.language = .vietnamese
        value.enterAction = .custom("Xong")
        searchKeyboard.presentation = value
    }

    /// Applies the one-shot Shift, which then drops back to lowercase.
    private func searchCharacter(for key: KeySpec) -> String {
        guard searchShiftState == .uppercase else { return key.label }
        searchShiftState = .lowercase
        return key.shiftedLabel ?? key.label.uppercased()
    }
}
#endif
