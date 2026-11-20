import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

/// The process-wide state a controller reads as if it were its own. Only
/// `KeyboardRuntime.adopt` changes the configuration, so these are read-only here.
extension KeyboardViewController {
    var runtime: KeyboardRuntime { .shared }

    var configuration: FunputConfiguration { runtime.configuration }
    var selectedTheme: KeyboardTheme { runtime.selectedTheme }
    var cachedThemedPresentation: KeyboardPresentation? { runtime.cachedThemedPresentation }
    var clipboardStore: ClipboardStore { runtime.clipboardStore }

    var backgroundImageCache: KeyboardBackgroundAssetCache<UIImage> { runtime.backgroundImageCache }
    var cachedBackgroundImage: UIImage? { runtime.backgroundImageCache.value }
    var resolvedBackgroundRequest: KeyboardBackgroundRequest? {
        get { runtime.resolvedBackgroundRequest }
        set { runtime.resolvedBackgroundRequest = newValue }
    }

    var configurationStore: FunputConfigurationStore { runtime.configurationStore }
    var customThemeStore: CustomThemeStore { runtime.customThemeStore }
    var themeAssetStore: ThemeAssetStore { runtime.themeAssetStore }
    var accessStateStore: KeyboardAccessStateStore { runtime.accessStateStore }
    var emojiRecentsStore: EmojiRecentsStore { runtime.emojiRecentsStore }
    var kaomojiRecentsStore: EmojiRecentsStore { runtime.kaomojiRecentsStore }
    var bootstrapSnapshotStore: KeyboardBootstrapSnapshotStore { runtime.bootstrapSnapshotStore }
}
