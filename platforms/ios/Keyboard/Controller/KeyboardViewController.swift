//
//  KeyboardViewController.swift
//  Keyboard
//
//  Created by P-Code Dynamics on 11/7/26.
//

import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import ThemeRuntime
import UIKit

final class KeyboardViewController: UIInputViewController {
    let inputCoordinator = KeyboardInputCoordinator()
    let keyboardView = KeyboardSurfaceView()
    let emojiView = EmojiKeyboardView()
    let kaomojiView = KaomojiKeyboardView()
    let clipboardPanelView = ClipboardKeyboardView()
    let emojiRecentsStore = EmojiRecentsStore()
    let clipboardStore = ClipboardStore()
    let kaomojiRecentsStore = EmojiRecentsStore(key: FunputAppGroup.kaomojiRecentsKey)
    let accessStateStore = KeyboardAccessStateStore()
    let customThemeStore = CustomThemeStore()
    let themeAssetStore = ThemeAssetStore()
    let personalSuggestionService = PersonalSuggestionService()
    var displayedSurface = KeyboardSurface.funput
    var configuration = FunputConfiguration.default
    var themeCatalog = ThemeCatalog()
    var cachedPresentationConfiguration: FunputConfiguration?
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

    override func viewDidLoad() {
        super.viewDidLoad()
        installKeyboardView()
        installPersonalSuggestions()
        reloadConfiguration()
        updateTextInputTraits(force: true)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        isKeyboardVisible = true
        activatePreferredHeight()
        refreshClipboardOffer()
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
        installEmojiView()
        installClipboard()
        installClipboardPanel()
        installKaomojiView()

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            emojiView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            emojiView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            emojiView.topAnchor.constraint(equalTo: view.topAnchor),
            emojiView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            kaomojiView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            kaomojiView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            kaomojiView.topAnchor.constraint(equalTo: view.topAnchor),
            kaomojiView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            clipboardPanelView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            clipboardPanelView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            clipboardPanelView.topAnchor.constraint(equalTo: view.topAnchor),
            clipboardPanelView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        heightController.install(on: view)
    }

    func updatePreferredHeight() {
        heightController.update(
            for: keyboardView.presentation,
            traits: traitCollection
        )
    }

    private func activatePreferredHeight() {
        heightController.activate(
            for: keyboardView.presentation,
            traits: traitCollection
        )
    }
}

enum KeyboardSurface {
    case funput
    case emoji
    case kaomoji
    case clipboard
}
