import FunputShared
import ThemeRuntime
import ThemeSchema
import UIKit

extension AppearanceModel {
    /// The key of a custom theme's picture in `mode`; bundled themes ship their own art.
    func snapshotKey(themeID: String, mode: AppearancePreviewMode) -> ThemeSnapshotKey? {
        guard let custom = catalog.customTheme(id: themeID) else { return nil }
        return ThemeSnapshotKey(theme: custom.theme, mode: mode, configuration: configuration)
    }

    /// The captured picture a gallery card shows for `theme` in the current preview mode.
    func snapshot(for theme: KeyboardTheme) -> UIImage? {
        snapshotKey(themeID: theme.id, mode: previewMode).flatMap(snapshots.image(for:))
    }
}
