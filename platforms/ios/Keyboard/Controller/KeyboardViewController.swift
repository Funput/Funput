//
//  KeyboardViewController.swift
//  Keyboard
//
//  Created by P-Code Dynamics on 11/7/26.
//

import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import PersonalSuggestions
import ThemeRuntime
import ThemeSchema
import UIKit

final class KeyboardViewController: UIInputViewController {
    let launchTrace = KeyboardLaunchTrace()
    let censusEnrollment = KeyboardLifetimeCensus.Enrollment(.controller)
    let inputCoordinator = KeyboardInputCoordinator()
    let shortcutsLoader = KeyboardShortcutsLoader(store: KeyboardShortcutSource())
    lazy var keyboardView = makePrimarySurface()
    var hasPrimarySurface = false
    var emojiView: EmojiKeyboardView?
    var kaomojiView: KaomojiKeyboardView?
    var clipboardPanelView: ClipboardKeyboardView?
    let emojiRecentsStore = EmojiRecentsStore()
    let emojiCatalogCache = EmojiCatalogCache()
    /// Rebuilt whenever configuration changes, since it carries the chosen expiry.
    /// Starts as a placeholder with no directory, so `init` makes no disk calls:
    /// bootstrap's first `adoptConfiguration` always installs the real store, before
    /// anything can touch the clipboard.
    var clipboardStore = ClipboardStore(directory: nil)
    let kaomojiRecentsStore = EmojiRecentsStore(key: FunputAppGroup.kaomojiRecentsKey)
    let accessStateStore = KeyboardAccessStateStore()
    let customThemeStore = CustomThemeStore()
    let themeAssetStore = ThemeAssetStore()
    /// The word list typo correction weighs candidates against. Separate from the
    /// store below, and read-only, so the correction handshake can ask it on the
    /// keystroke path without waiting on that store's queue.
    let correctionWords = CorrectionWordSource()
    let personalSuggestionService = PersonalSuggestionService {
        PersonalSuggestionWorker(onResult: $0)
    }
    let activationState = KeyboardActivationState()
    let backgroundImageCache = KeyboardBackgroundAssetCache<UIImage>()
    /// Lazy because its initializer prepares the App Group directory, and `init` makes
    /// no disk calls; bootstrap reads it first, in `viewDidLoad`.
    lazy var bootstrapSnapshotStore = KeyboardBootstrapSnapshotStore()
    var clipboardRetryTask: Task<Void, Never>?
    let clipboardChangeMonitor = ClipboardChangeMonitor()
    var clipboardStartTask: Task<Void, Never>?
    var isClipboardMonitoring = false
    lazy var clipboardRecorder = makeClipboardRecorder()
    var pendingBootstrapRepair: KeyboardBootstrapSnapshot?
    var bootstrappedSource: KeyboardBootstrappedSource?
    var adoptedIdentity: KeyboardActivationIdentity?
    var resolvedBackgroundRequest: KeyboardBackgroundRequest?
    var displayedSurface = KeyboardSurface.funput
    var configuration = FunputConfiguration.default
    var themeCatalog = ThemeCatalog()
    var selectedTheme: KeyboardTheme = BundledThemes.default
    var currentPresentation = KeyboardPresentation()
    var cachedThemedPresentation: KeyboardPresentation?
    let configurationStore = FunputConfigurationStore()
#if DEBUG
    lazy var touchDiagnosticsReporter = KeyboardTouchDiagnosticsReporter(
        surface: keyboardView
    )
#endif
    var resolvedTextInputTraits = KeyboardInputContext(
        editorMode: .text,
        enterAction: .newLine,
        initialLayoutMode: .letters
    )
    let heightController = KeyboardHeightController()
    private(set) var isKeyboardVisible = false

    var cachedBackgroundImage: UIImage? { backgroundImageCache.value }

    override func loadView() {
        inputView = KeyboardAudioInputView(frame: .zero, inputViewStyle: .keyboard)
    }

    override func viewDidLoad() {
        launchTrace.beginViewDidLoad()
        super.viewDidLoad()
        view.isOpaque = false
        view.backgroundColor = .clear
        inputView?.allowsSelfSizing = true
        heightController.install(on: view)
        // The height only tracks the size class, and `viewWillTransition` alone misses
        // every change that is not a rotation of an already-visible keyboard.
        if #available(iOS 17, *) {
            registerForTraitChanges([UITraitVerticalSizeClass.self]) {
                (controller: KeyboardViewController, _) in
                controller.updatePreferredHeight()
            }
        }
        bootstrapKeyboard()
        launchTrace.endViewDidLoad()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        launchTrace.finish()
        releaseSystemGestureTouchDelay()
        if hasFullAccess { accessStateStore.recordFullAccess() }
#if DEBUG
        touchDiagnosticsReporter.startIfAvailable(hasFullAccess: hasFullAccess)
#endif
        repairBootstrapSnapshotIfNeeded()
        scheduleClipboardMonitoringStart()
        KeyboardMemoryTrace.recordAppearance()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        shortcutsLoader.cancel()
        // The next activation starts on the letters anyway; an open panel would only
        // hold memory while the keyboard is off screen.
        switchSurface(to: .funput)
        emojiCatalogCache.release()
        detachPrimarySurface()
        KeyboardMemoryTrace.record("Disappeared")
#if DEBUG
        touchDiagnosticsReporter.finish()
#endif
    }

    func markPreferredHeightHidden() {
        isKeyboardVisible = false
    }

    func markPreferredHeightVisible() {
        isKeyboardVisible = true
    }
}
