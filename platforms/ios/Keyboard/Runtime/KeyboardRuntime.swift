import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

/// What the keyboard builds once per process and keeps across host apps.
///
/// iOS creates a `KeyboardViewController` for every host app inside the same
/// extension process. Nothing here depends on which app is showing the keyboard, so
/// rebuilding it per controller made each app switch cost what a cold start costs,
/// and briefly held two of everything. A controller keeps only what belongs to its
/// own presentation and to the document it is typing into.
@MainActor
final class KeyboardRuntime {
    static let shared = KeyboardRuntime()

    let surfaceHost = KeyboardSurfaceHost()
    let backgroundImageCache = KeyboardBackgroundAssetCache<UIImage>()
    var resolvedBackgroundRequest: KeyboardBackgroundRequest?

    private(set) var configuration = FunputConfiguration.default
    private(set) var selectedTheme: KeyboardTheme = BundledThemes.default
    private(set) var cachedThemedPresentation: KeyboardPresentation?
    private var adoptedIdentity: KeyboardActivationIdentity?
    /// Rebuilt whenever configuration changes, since it carries the chosen expiry.
    /// Starts as a placeholder with no directory, so creating the runtime makes no
    /// disk calls: the first `adopt` always installs the real store, before anything
    /// can touch the clipboard.
    private(set) var clipboardStore = ClipboardStore(directory: nil)

    let configurationStore = FunputConfigurationStore()
    let customThemeStore = CustomThemeStore()
    let themeAssetStore = ThemeAssetStore()
    let accessStateStore = KeyboardAccessStateStore()
    let emojiRecentsStore = EmojiRecentsStore()
    let kaomojiRecentsStore = EmojiRecentsStore(key: FunputAppGroup.kaomojiRecentsKey)
    /// Lazy because the first controller's `init` reaches the runtime, and `init`
    /// makes no disk calls; bootstrap reads it first, in `viewDidLoad`.
    lazy var bootstrapSnapshotStore = KeyboardBootstrapSnapshotStore()

    private init() {}

    /// Installs one activation's configuration and theme.
    ///
    /// Rebuilding the themed presentation means resolving the theme, so an activation
    /// that changed nothing keeps the last one. That now holds across host apps, which
    /// is why the identity includes the one accessibility setting the resolved theme
    /// bakes in.
    func adopt(_ source: KeyboardActivationSource) {
        let identity = KeyboardActivationIdentity(
            configuration: source.configuration,
            selectedTheme: source.selectedTheme,
            reducesTransparency: UIAccessibility.isReduceTransparencyEnabled
        )
        guard adoptedIdentity != identity else { return }
        adoptedIdentity = identity
        configuration = source.configuration
        selectedTheme = source.selectedTheme
        cachedThemedPresentation = KeyboardPresentationFactory.make(
            from: source.configuration,
            catalog: source.catalog
        )
        clipboardStore = ClipboardStore(expiry: source.configuration.clipboardExpiry)
    }

    /// Drops the decoded backdrop, the largest allocation the keyboard holds. The
    /// surface keeps its own reference to the image, so the cache alone is not enough.
    func releaseBackgroundImage() {
        backgroundImageCache.clear()
        resolvedBackgroundRequest = nil
        surfaceHost.surface?.backgroundImage = nil
    }
}
