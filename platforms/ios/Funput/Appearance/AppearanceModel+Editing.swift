import FunputShared
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema

extension AppearanceModel {
    func editorRequest() -> ThemeEditorRequest {
        if let custom = previewCustomTheme {
            return .edit(customThemeID: custom.id)
        }
        return .create(baseThemeID: previewTheme.id)
    }

    func makeDraft(for request: ThemeEditorRequest) -> ThemeEditorDraft {
        switch request {
        case .create(let baseID):
            let base = BundledThemes.theme(id: baseID) ?? BundledThemes.default
            let custom = CustomKeyboardTheme(baseTheme: base)
            return ThemeEditorDraft(
                initialTheme: custom,
                isNew: true,
                baseTheme: base,
                customTheme: custom,
                previewMode: previewMode
            )
        case .edit(let customID):
            let custom = catalog.customTheme(id: customID)
                ?? CustomKeyboardTheme(baseTheme: BundledThemes.default)
            let base = catalog.baseTheme(for: custom)
            return ThemeEditorDraft(
                initialTheme: custom,
                isNew: false,
                baseTheme: base,
                customTheme: custom,
                previewMode: previewMode
            )
        }
    }

    func presentation(for draft: ThemeEditorDraft) -> KeyboardPresentation {
        var candidate = configuration
        candidate.selectedThemeID = draft.customTheme.id
        let others = customThemes.filter { $0.id != draft.customTheme.id }
        let draftCatalog = ThemeCatalog(customThemes: others + [draft.customTheme])
        return KeyboardPreviewPresentation.make(configuration: candidate, catalog: draftCatalog)
    }

    func save(_ draft: ThemeEditorDraft) -> Bool {
        var custom = draft.customTheme
        custom.theme.metadata.name = draft.trimmedName
        custom.theme.schemaVersion = KeyboardTheme.currentSchemaVersion
        guard draft.canSave, customStore.upsert(custom) else {
            showsSaveError = true
            return false
        }
        refreshCustomThemes()
        previewThemeID = custom.id
        return true
    }

    func deletePreviewCustomTheme() -> Bool {
        guard let custom = previewCustomTheme else { return false }
        let baseID = catalog.baseTheme(for: custom).id
        if appliedThemeID == custom.id {
            guard replaceAppliedTheme(with: baseID) else { return false }
        }
        guard customStore.delete(id: custom.id) else {
            if appliedThemeID == baseID { _ = replaceAppliedTheme(with: custom.id) }
            showsSaveError = true
            return false
        }
        refreshCustomThemes()
        previewThemeID = baseID
        return true
    }
}
