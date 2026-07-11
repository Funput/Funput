//
//  KeyboardViewController.swift
//  Keyboard
//
//  Created by P-Code Dynamics on 11/7/26.
//

import KeyboardLayout
import KeyboardRenderer
import UIKit

final class KeyboardViewController: UIInputViewController {
    private let keyboardView = KeyboardSurfaceView()
    private var heightConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()
        installKeyboardView()
        updatePresentation()
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        updatePresentation()
        updatePreferredHeight()
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        coordinator.animate { [weak self] _ in
            self?.updatePreferredHeight()
        }
    }

    private func installKeyboardView() {
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.onKeyEvent = { _ in }
        view.addSubview(keyboardView)

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let constraint = view.heightAnchor.constraint(equalToConstant: 280)
        constraint.priority = .init(999)
        constraint.isActive = true
        heightConstraint = constraint
    }

    private func updatePresentation() {
        var presentation = keyboardView.presentation
        presentation.layout = .funputQWERTY
        presentation.showsInputModeKey = needsInputModeSwitchKey
        keyboardView.presentation = presentation
    }

    private func updatePreferredHeight() {
        heightConstraint?.constant = KeyboardMetrics.recommendedHeight(
            for: traitCollection,
            scale: keyboardView.presentation.sizing.heightScale
        )
    }
}
