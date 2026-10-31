#if canImport(UIKit)
import UIKit

/// The long-press menu on a suggested word, and the same choices for VoiceOver.
///
/// Only a slot holding a stored word gets one: a correction or an undo chip is not
/// something the dictionary holds. The menu uses the system's own chrome, as the
/// clipboard panel's does, so it reads the same on every theme.
extension KeyboardSuggestionBarView: UIContextMenuInteractionDelegate {
    /// Built here rather than in the delegate so tests can reach it without a gesture.
    func menu(forSlot index: Int) -> UIMenu? {
        guard let candidate = editableCandidate(at: index) else { return nil }
        let actions = KeyboardSuggestionAction.allCases.map { action in
            UIAction(
                title: action.title,
                image: UIImage(systemName: action.symbolName),
                attributes: action.isDestructive ? .destructive : []
            ) { [weak self] _ in self?.onEdit?(candidate, action) }
        }
        return UIMenu(title: candidate.text, children: actions)
    }

    func accessibilityActions(forSlot index: Int) -> [UIAccessibilityCustomAction]? {
        guard let candidate = editableCandidate(at: index) else { return nil }
        return KeyboardSuggestionAction.allCases.map { action in
            UIAccessibilityCustomAction(name: action.title) { [weak self] _ in
                self?.onEdit?(candidate, action)
                return true
            }
        }
    }

    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        configurationForMenuAtLocation location: CGPoint
    ) -> UIContextMenuConfiguration? {
        guard let index = interaction.view?.tag, let menu = menu(forSlot: index) else { return nil }
        return UIContextMenuConfiguration(actionProvider: { _ in menu })
    }

    private func editableCandidate(at index: Int) -> KeyboardSuggestionCandidate? {
        guard onEdit != nil, candidates.indices.contains(index) else { return nil }
        let candidate = candidates[index]
        return candidate.kind == .suggestion ? candidate : nil
    }
}
#endif
