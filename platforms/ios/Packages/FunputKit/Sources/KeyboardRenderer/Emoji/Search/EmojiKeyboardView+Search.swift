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
        searchQuery = ""
        updateSearchUI()
    }

    func resetSearch() {
        searchState = .browsing
        searchQuery = ""
        searchShiftState = .lowercase
        updateSearchUI()
    }

    func handleSearchKey(_ event: KeyboardKeyEvent) {
        guard event.phase == .released || event.phase == .repeated else { return }
        switch event.key.role {
        case .character:
            appendSearchCharacter(event.key)
        case .space:
            appendSearchSpace()
        case .backspace:
            if !searchQuery.isEmpty { searchQuery.removeLast() }
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
        value.layout = EmojiSearchKeyboardLayout.layout
        value.shiftState = searchShiftState
        value.language = .vietnamese
        value.enterAction = .custom("Xong")
        searchKeyboard.presentation = value
    }

    private func appendSearchCharacter(_ key: KeySpec) {
        let value = searchShiftState == .lowercase
            ? key.label
            : (key.shiftedLabel ?? key.label.uppercased())
        searchQuery.append(contentsOf: value)
        if searchShiftState == .uppercase { searchShiftState = .lowercase }
    }

    private func appendSearchSpace() {
        guard !searchQuery.isEmpty, !searchQuery.hasSuffix(" ") else { return }
        searchQuery.append(" ")
    }
}
#endif
