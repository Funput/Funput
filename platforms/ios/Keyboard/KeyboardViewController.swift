//
//  KeyboardViewController.swift
//  Keyboard
//
//  Created by P-Code Dynamics on 11/7/26.
//

import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

final class KeyboardViewController: UIInputViewController {
    let inputCoordinator = KeyboardInputCoordinator(inputMethod: .vni)
    let keyboardView = KeyboardSurfaceView()
    private var heightConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()
        updateInputPresentation()
        installKeyboardView()
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
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.onKeyEvent = { [weak self] event in
            self?.handleKeyEvent(event)
        }
        view.addSubview(keyboardView)

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
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
