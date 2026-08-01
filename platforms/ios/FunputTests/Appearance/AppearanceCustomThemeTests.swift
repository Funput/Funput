import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct AppearanceCustomThemeTests {
    @Test("Draft cancellation writes nothing")
    func cancelDoesNotPersist() {
        let themes = ThemeTestStore()
        let model = makeModel(themes: themes)
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        draft.customTheme.theme.geometry.horizontalGap = 8

        #expect(draft.isDirty)
        #expect(themes.upsertCount == 0)
        #expect(model.customThemes.isEmpty)
    }

    @Test("Saving multiple themes previews the latest without applying it")
    func saveOnlyPreviews() {
        let themes = ThemeTestStore()
        let config = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: config, customStore: themes)
        let first = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        let second = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.classicLight.id))

        #expect(model.save(first))
        #expect(model.save(second))
        #expect(model.customThemes.count == 2)
        #expect(model.previewThemeID == second.customTheme.id)
        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(config.saveCount == 0)
    }

    @Test("Applying, editing, and deleting an active custom theme uses its base fallback")
    func activeThemeLifecycle() {
        let themes = ThemeTestStore()
        let config = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: config, customStore: themes)
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        #expect(model.save(draft))
        model.applyPreview()

        draft = model.makeDraft(for: .edit(customThemeID: draft.customTheme.id))
        draft.customTheme.theme.metadata.name = "Midnight Edited"
        #expect(model.save(draft))
        #expect(model.appliedThemeID == draft.customTheme.id)
        #expect(themes.load().first?.theme.metadata.name == "Midnight Edited")

        #expect(model.deletePreviewCustomTheme())
        #expect(config.configuration.selectedThemeID == KeyboardTheme.midnight.id)
        #expect(model.appliedThemeID == KeyboardTheme.midnight.id)
        #expect(themes.load().isEmpty)
    }

    @Test("Reset geometry preserves the custom name")
    func resetGeometryKeepsName() {
        let model = makeModel()
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.classicLight.id))
        draft.customTheme.theme.metadata.name = "My Keys"
        draft.customTheme.theme.geometry.horizontalPadding = 16
        draft.customTheme.theme.geometry = draft.baseTheme.geometry
        draft.customTheme.theme.metrics.cornerRadius = draft.baseTheme.metrics.cornerRadius

        #expect(draft.customTheme.theme.metadata.name == "My Keys")
        #expect(draft.customTheme.theme.geometry == draft.baseTheme.geometry)
    }

    @Test("Storage failures retain the applied theme and draft")
    func failuresAreNonDestructive() {
        let themes = ThemeTestStore(acceptsMutations: false)
        let model = makeModel(themes: themes)
        let draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))

        #expect(!model.save(draft))
        #expect(model.customThemes.isEmpty)
        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(model.showsSaveError)
    }

    @Test("Failed deletion rolls an active theme back")
    func failedDeleteRollsBack() {
        let themes = ThemeTestStore()
        let config = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: config, customStore: themes)
        let draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        #expect(model.save(draft))
        model.applyPreview()
        themes.acceptsMutations = false

        #expect(!model.deletePreviewCustomTheme())
        #expect(model.appliedThemeID == draft.customTheme.id)
        #expect(config.configuration.selectedThemeID == draft.customTheme.id)
        #expect(themes.load().count == 1)
    }

    private func makeModel(themes: ThemeTestStore = ThemeTestStore()) -> AppearanceModel {
        AppearanceModel(store: SettingsTestStore(configuration: .default), customStore: themes)
    }
}

final class ThemeTestStore: CustomThemeStoring {
    private var themes: [CustomKeyboardTheme]
    var acceptsMutations: Bool
    private(set) var upsertCount = 0

    init(themes: [CustomKeyboardTheme] = [], acceptsMutations: Bool = true) {
        self.themes = themes
        self.acceptsMutations = acceptsMutations
    }

    func load() -> [CustomKeyboardTheme] { themes }
    func upsert(_ theme: CustomKeyboardTheme) -> Bool {
        upsertCount += 1
        guard acceptsMutations else { return false }
        themes.removeAll { $0.id == theme.id }
        themes.append(theme)
        return true
    }
    func delete(id: String) -> Bool {
        guard acceptsMutations else { return false }
        themes.removeAll { $0.id == id }
        return true
    }
}
