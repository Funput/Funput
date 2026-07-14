import Foundation
import FunputShared
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

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
                previewMode: previewMode,
                sourceImageData: nil,
                renderedImageData: nil
            )
        case .edit(let customID):
            let custom = catalog.customTheme(id: customID)
                ?? CustomKeyboardTheme(baseTheme: BundledThemes.default)
            let base = catalog.baseTheme(for: custom)
            let assetID = custom.theme.backgroundEffects.image?.assetID
            let source = assetID.flatMap(assetStore.sourceData)
            let rendered = assetID.flatMap(assetStore.renderedData)
            return ThemeEditorDraft(
                initialTheme: custom,
                isNew: false,
                baseTheme: base,
                customTheme: custom,
                previewMode: previewMode,
                sourceImageData: source,
                renderedImageData: rendered,
                imageDataIsValid: source.flatMap(UIImage.init(data:)) != nil
                    && rendered.flatMap(UIImage.init(data:)) != nil
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
        guard draft.canSave else { showsSaveError = true; return false }
        let oldAssetID = draft.initialTheme.theme.backgroundEffects.image?.assetID
        var newAssetID: String?
        if draft.needsAssetSave {
            guard let source = draft.sourceImageData,
                  let rendered = draft.renderedImageData,
                  let savedID = assetStore.save(source: source, rendered: rendered, id: UUID())
            else { showsSaveError = true; return false }
            newAssetID = savedID
            custom.theme.backgroundEffects.image?.assetID = savedID
        }
        guard customStore.upsert(custom) else {
            if let newAssetID { _ = assetStore.delete(assetID: newAssetID) }
            showsSaveError = true
            return false
        }
        refreshCustomThemes()
        previewThemeID = custom.id
        let currentID = custom.theme.backgroundEffects.image?.assetID
        if let oldAssetID, oldAssetID != currentID { _ = assetStore.delete(assetID: oldAssetID) }
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
        if let assetID = custom.theme.backgroundEffects.image?.assetID {
            _ = assetStore.delete(assetID: assetID)
        }
        refreshCustomThemes()
        previewThemeID = baseID
        return true
    }
}
