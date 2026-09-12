#if DEBUG
import SwiftUI
import UIKit

/// Observes an attempted sheet swipe as well as preventing unsaved dismissal.
struct ShortcutDismissGuard: UIViewControllerRepresentable {
    let hasChanges: Bool
    let onAttempt: () -> Void

    func makeUIViewController(context: Context) -> Controller { Controller() }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.hasChanges = hasChanges
        controller.onAttempt = onAttempt
        controller.attach()
    }

    final class Controller: UIViewController, UIAdaptivePresentationControllerDelegate {
        var hasChanges = false
        var onAttempt: (() -> Void)?

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            attach()
        }

        func attach() {
            var ancestor: UIViewController? = parent
            while let current = ancestor {
                if let presentation = current.presentationController {
                    presentation.delegate = self
                }
                ancestor = current.parent
            }
        }

        func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
            !hasChanges
        }

        func presentationControllerDidAttemptToDismiss(_ presentationController: UIPresentationController) {
            onAttempt?()
        }
    }
}

#endif
