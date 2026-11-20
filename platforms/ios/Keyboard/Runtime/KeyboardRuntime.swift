import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import PersonalSuggestions
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

    /// The word list typo correction weighs candidates against. Separate from the
    /// service below, and read-only, so the correction handshake can ask it on the
    /// keystroke path without waiting on that service's queue. The coordinator holds
    /// it weakly; this is what keeps it alive.
    let correctionWords = CorrectionWordSource()
    /// One service, so one worker and one open store, however many host apps have a
    /// controller alive.
    let personalSuggestionService = PersonalSuggestionService {
        PersonalSuggestionWorker(onResult: $0)
    }

    private init() {}

    /// The surface has passed to another host app's controller: nothing typed in the
    /// last app may be counted, kept or suggested in this one.
    func beginTypingSession(hasFullAccess: Bool) {
        correctionWords.forgetSession()
        personalSuggestionService.beginSession(hasFullAccess: hasFullAccess)
    }

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
