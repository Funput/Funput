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
import UIKit

final class KeyboardViewController: UIInputViewController {
    let inputCoordinator = KeyboardInputCoordinator()
    let keyboardView = KeyboardSurfaceView()
    let emojiView = EmojiKeyboardView()
    let emojiRecentsStore = EmojiRecentsStore()
    let accessStateStore = KeyboardAccessStateStore()
    var displayedSurface = KeyboardSurface.funput
    var configuration = FunputConfiguration.default
    let configurationStore = FunputConfigurationStore()
    var resolvedTextInputTraits = ResolvedTextInputTraits(
        editorMode: .text,
        enterAction: .newLine,
        initialLayoutMode: .letters
    )
    private var heightConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()
        installKeyboardView()
        reloadConfiguration()
        updateTextInputTraits(force: true)
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        updatePreferredHeight()
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        coordinator.animate { [weak self] _ in
            self?.updatePreferredHeight()
        }
    }

    private func installKeyboardView() {
        view.isOpaque = false
        view.backgroundColor = .clear
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.onKeyEvent = { [weak self] event in
            self?.handleKeyEvent(event)
        }
        keyboardView.onSystemInputModeEvent = { [weak self] source, event in
            self?.handleSystemInputModeEvent(from: source, event: event)
        }
        view.addSubview(keyboardView)
        installEmojiView()

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            emojiView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            emojiView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            emojiView.topAnchor.constraint(equalTo: view.topAnchor),
            emojiView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let constraint = view.heightAnchor.constraint(
            equalToConstant: KeyboardMetrics.phonePortraitBaseHeight
        )
        constraint.priority = .init(999)
        constraint.isActive = true
        heightConstraint = constraint
    }

    func updatePreferredHeight() {
        heightConstraint?.constant = KeyboardMetrics.recommendedHeight(
            for: keyboardView.presentation.layout,
            traits: traitCollection,
            scale: keyboardView.presentation.sizing.heightScale
        )
    }
}

enum KeyboardSurface {
    case funput
    case emoji
}
