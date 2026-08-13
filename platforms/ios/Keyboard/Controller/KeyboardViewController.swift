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
    let keyboardView = KeyboardSurfaceView()
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
    private let heightController = KeyboardHeightController()
    private var isKeyboardVisible = false

    var cachedBackgroundImage: UIImage? { backgroundImageCache.value }

    override func viewDidLoad() {
        launchTrace.beginViewDidLoad()
        super.viewDidLoad()
        installKeyboardView()
        installPersonalSuggestions()
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

    func deactivatePreferredHeight() {
        isKeyboardVisible = false
        heightController.deactivate()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
#if DEBUG
        touchDiagnosticsReporter.finish()
#endif
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        let shouldReactivate = heightController.deactivate()
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in
            guard let self, shouldReactivate, isKeyboardVisible else { return }
            updatePreferredHeight()
            activatePreferredHeight()
        }
    }

    private func installKeyboardView() {
        view.isOpaque = false
        view.backgroundColor = .clear
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.onKeyEvent = { [weak self] event in
            self?.handleKeyEvent(event)
        }
        view.addSubview(keyboardView)
        installClipboard()

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        heightController.install(on: view)
    }

    func updatePreferredHeight() {
        heightController.update(
            for: currentPresentation,
            traits: traitCollection
        )
    }

    func activatePreferredHeightForAppearance() {
        updatePreferredHeight()
        isKeyboardVisible = true
        activatePreferredHeight()
        launchTrace.recordHeightReady()
    }

    private func activatePreferredHeight() {
        heightController.activate()
    }
}

enum KeyboardSurface: String {
    case funput
    case emoji
    case kaomoji
    case clipboard
}
