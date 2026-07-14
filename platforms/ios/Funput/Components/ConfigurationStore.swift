import Foundation
import FunputShared
import ThemeRuntime
import ThemeSchema

protocol FunputConfigurationStoring {
    func load() -> FunputConfiguration
    func save(_ configuration: FunputConfiguration) -> Bool
}

extension FunputConfigurationStore: FunputConfigurationStoring {}

protocol KeyboardAccessStateReading {
    var hasObservedFullAccess: Bool { get }
}

extension KeyboardAccessStateStore: KeyboardAccessStateReading {}

protocol CustomThemeStoring {
    func load() -> [CustomKeyboardTheme]
    func upsert(_ theme: CustomKeyboardTheme) -> Bool
    func delete(id: String) -> Bool
}

extension CustomThemeStore: CustomThemeStoring {}

protocol ThemeAssetStoring {
    func save(source: Data, rendered: Data, id: UUID) -> String?
    func sourceData(for assetID: String) -> Data?
    func renderedData(for assetID: String) -> Data?
    func delete(assetID: String) -> Bool
    func cleanup(referencedAssetIDs: Set<String>)
}

extension ThemeAssetStore: ThemeAssetStoring {}

struct PreviewConfigurationStore: FunputConfigurationStoring {
    var configuration = FunputConfiguration.default

    func load() -> FunputConfiguration { configuration }
    func save(_ configuration: FunputConfiguration) -> Bool { true }
}

final class PreviewCustomThemeStore: CustomThemeStoring {
    var themes: [CustomKeyboardTheme]

    init(themes: [CustomKeyboardTheme] = []) {
        self.themes = themes
    }

    func load() -> [CustomKeyboardTheme] { themes }

    func upsert(_ theme: CustomKeyboardTheme) -> Bool {
        themes.removeAll { $0.id == theme.id }
        themes.append(theme)
        return true
    }

    func delete(id: String) -> Bool {
        themes.removeAll { $0.id == id }
        return true
    }
}

final class PreviewThemeAssetStore: ThemeAssetStoring {
    private var assets: [String: (Data, Data)] = [:]
    var assetCount: Int { assets.count }

    func save(source: Data, rendered: Data, id: UUID) -> String? {
        let key = id.uuidString.lowercased()
        assets[key] = (source, rendered)
        return key
    }

    func sourceData(for assetID: String) -> Data? { assets[assetID]?.0 }
    func renderedData(for assetID: String) -> Data? { assets[assetID]?.1 }
    func delete(assetID: String) -> Bool { assets.removeValue(forKey: assetID) != nil }
    func cleanup(referencedAssetIDs: Set<String>) {
        assets = assets.filter { referencedAssetIDs.contains($0.key) }
    }
}
