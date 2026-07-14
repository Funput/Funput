import Foundation
import ThemeSchema

enum ThemeEditorRequest: Identifiable {
    case create(baseThemeID: String)
    case edit(customThemeID: String)

    var id: String {
        switch self {
        case .create(let id): "create-\(id)"
        case .edit(let id): "edit-\(id)"
        }
    }
}

struct ThemeEditorDraft: Equatable {
    let initialTheme: CustomKeyboardTheme
    let isNew: Bool
    let baseTheme: KeyboardTheme
    var customTheme: CustomKeyboardTheme
    var previewMode: AppearancePreviewMode

    var isDirty: Bool { customTheme != initialTheme }
    var trimmedName: String { customTheme.theme.metadata.name.trimmingCharacters(in: .whitespacesAndNewlines) }
    var canSave: Bool { (1...40).contains(trimmedName.count) }

    mutating func resetToBase() {
        customTheme.theme.geometry = baseTheme.geometry
        customTheme.theme.palette = baseTheme.palette
        customTheme.theme.colorEffects = baseTheme.colorEffects
        customTheme.theme.metrics.cornerRadius = baseTheme.metrics.cornerRadius
        customTheme.theme.metrics.pressedScale = baseTheme.metrics.pressedScale
    }
}
