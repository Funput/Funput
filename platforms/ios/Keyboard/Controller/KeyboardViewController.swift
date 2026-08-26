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
    let inputCoordinator = KeyboardInputCoordinator()
    lazy var keyboardView = makePrimarySurface()
    var hasPrimarySurface = false
    var emojiView: EmojiKeyboardView?
    var kaomojiView: KaomojiKeyboardView?
    var clipboardPanelView: ClipboardKeyboardView?
    let emojiRecentsStore = EmojiRecentsStore()
    /// Rebuilt whenever configuration changes, since it carries the chosen expiry.
    var clipboardStore = ClipboardStore()
    let kaomojiRecentsStore = EmojiRecentsStore(key: FunputAppGroup.kaomojiRecentsKey)
    let accessStateStore = KeyboardAccessStateStore()
    let customThemeStore = CustomThemeStore()
    let themeAssetStore = ThemeAssetStore()
    let personalSuggestionService = PersonalSuggestionService {
        PersonalSuggestionWorker(onResult: $0)
    }
    let activationState = KeyboardActivationState()
    let backgroundImageCache = KeyboardBackgroundAssetCache<UIImage>()
    let bootstrapSnapshotStore = KeyboardBootstrapSnapshotStore()
    var clipboardRetryTask: Task<Void, Never>?
    var pendingBootstrapRepair: KeyboardBootstrapSnapshot?
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

    override func viewDidLoad() {
        launchTrace.beginViewDidLoad()
        super.viewDidLoad()
        view.isOpaque = false
        view.backgroundColor = .clear
        inputView?.allowsSelfSizing = true
        heightController.install(on: view)
        // The height only tracks the size class, and `viewWillTransition` alone misses
        // every change that is not a rotation of an already-visible keyboard.
        registerForTraitChanges([UITraitVerticalSizeClass.self]) {
            (controller: KeyboardViewController, _) in
            controller.updatePreferredHeight()
        }
        bootstrapKeyboard()
        launchTrace.endViewDidLoad()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        launchTrace.finish()
        if hasFullAccess { accessStateStore.recordFullAccess() }
#if DEBUG
        touchDiagnosticsReporter.startIfAvailable(hasFullAccess: hasFullAccess)
#endif
        refreshClipboardOffer()
        repairBootstrapSnapshotIfNeeded()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
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

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        let shouldReactivate = heightController.deactivate()
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in
            guard let self, shouldReactivate else { return }
            updatePreferredHeight()
            activatePreferredHeight()
        }
    }

}

enum KeyboardSurface: String {
    case funput
    case emoji
    case kaomoji
    case clipboard
}
