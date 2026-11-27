import Foundation
import FunputShared
import Testing
import ThemeRuntime
import ThemeSchema
import UIKit
@testable import Funput

@MainActor
struct ThemeSnapshotTests {
    @Test("A renamed theme keeps its picture; a restyled one or a changed setting does not")
    func fingerprintFollowsLooksOnly() {
        let theme = KeyboardTheme.midnight
        let key = ThemeSnapshotKey(theme: theme, mode: .dark, configuration: .default)
        var renamed = theme
        renamed.metadata.name = "Another name"
        var restyled = theme
        restyled.geometry.horizontalGap += 3
        var numberRow = FunputConfiguration.default
        numberRow.showsNumberRow.toggle()
        var pinned = FunputConfiguration.default
        pinned.keyboardAppearance = .dark

        #expect(ThemeSnapshotKey(theme: renamed, mode: .dark, configuration: .default) == key)
        #expect(ThemeSnapshotKey(theme: restyled, mode: .dark, configuration: .default) != key)
        #expect(ThemeSnapshotKey(theme: theme, mode: .dark, configuration: numberRow) != key)
        #expect(ThemeSnapshotKey(theme: theme, mode: .dark, configuration: pinned) != key)
        #expect(ThemeSnapshotKey(theme: theme, mode: .light, configuration: .default) != key)
    }

    @Test("The file store keeps one picture per theme and style")
    func fileStoreReplacesAndCleansUp() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let store = ThemeSnapshotFileStore(directory: directory)
        var restyled = KeyboardTheme.midnight
        restyled.geometry.horizontalGap += 3
        let old = ThemeSnapshotKey(theme: .midnight, mode: .dark, configuration: .default)
        let new = ThemeSnapshotKey(theme: restyled, mode: .dark, configuration: .default)
        let light = ThemeSnapshotKey(theme: .midnight, mode: .light, configuration: .default)
        let other = ThemeSnapshotKey(theme: .classicLight, mode: .dark, configuration: .default)

        #expect(store.save(Data([1]), for: old))
        #expect(store.save(Data([2]), for: light))
        #expect(store.save(Data([3]), for: other))
        #expect(store.save(Data([4]), for: new))
        #expect(store.data(for: old) == nil)
        #expect(store.data(for: new) == Data([4]))
        #expect(store.data(for: light) == Data([2]))

        store.cleanup(keeping: [KeyboardTheme.midnight.id])
        #expect(store.data(for: other) == nil)
        #expect(store.data(for: light) == Data([2]))
        store.cleanup(keeping: [])
        #expect(try FileManager.default.contentsOfDirectory(atPath: directory.path).isEmpty)
    }

    @Test("A picture looked up before it existed shows once it is saved")
    func librarySeesNewPictures() {
        let library = ThemeSnapshotLibrary(storage: PreviewThemeSnapshotStore())
        let key = ThemeSnapshotKey(theme: .midnight, mode: .dark, configuration: .default)

        #expect(library.image(for: key) == nil)
        library.save(picture(), for: key)
        #expect(library.image(for: key) != nil)
    }

    @Test("A card shows its picture until the theme changes, and loses it when deleted")
    func pictureLifecycle() throws {
        let storage = PreviewThemeSnapshotStore()
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore(),
            snapshotStore: storage
        )
        model.previewMode = .dark
        #expect(model.save(model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))))
        let saved = model.customThemes[0].theme
        let key = try #require(model.snapshotKey(themeID: saved.id, mode: .dark))
        model.snapshots.save(picture(), for: key)

        #expect(model.snapshot(for: saved) != nil)
        model.previewMode = .light
        #expect(model.snapshot(for: saved) == nil)
        #expect(model.snapshotKey(themeID: KeyboardTheme.midnight.id, mode: .dark) == nil)

        model.previewMode = .dark
        var draft = model.makeDraft(for: .edit(customThemeID: saved.id))
        draft.customTheme.theme.geometry.horizontalGap += 3
        #expect(model.save(draft))
        #expect(model.snapshot(for: model.customThemes[0].theme) == nil)

        let edited = try #require(model.snapshotKey(themeID: saved.id, mode: .dark))
        model.snapshots.save(picture(), for: edited)
        #expect(storage.data(for: edited) != nil)
        #expect(model.deletePreviewCustomTheme())
        #expect(storage.data(for: edited) == nil)
    }

    private func picture() -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 4, height: 4)).image { context in
            UIColor.systemTeal.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 4, height: 4))
        }
    }
}
