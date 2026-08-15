import FunputShared
import ThemeSchema

extension AppearanceModel {
    func commit(themeID: String) {
        var candidate = configuration
        candidate.selectedThemeID = validThemeID(themeID)
        guard persistConfiguration(candidate) else { return }
        acceptPersistedConfiguration(candidate, updatesPreview: true)
    }

    func persistConfiguration(_ candidate: FunputConfiguration) -> Bool {
        let previous = configuration
        guard store.save(candidate) else {
            showsSaveError = true
            return false
        }
        guard bootstrap.save(
            configuration: candidate,
            customThemes: customThemes
        ) else {
            _ = store.save(previous)
            showsSaveError = true
            return false
        }
        return true
    }

    func restoreThemeMutation(
        previous: CustomKeyboardTheme?,
        mutatedID: String
    ) {
        if let previous {
            _ = customStore.upsert(previous)
        } else {
            _ = customStore.delete(id: mutatedID)
        }
    }
}
