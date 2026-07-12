import FunputShared
import KeyboardInput
import UIKit

extension KeyboardViewController {
    /// Reloads shared configuration and applies it to the engine and surface.
    ///
    /// Called on load and on every activation so preference changes made in the
    /// containing app take effect the next time the keyboard appears. Reading on
    /// activation (not per keystroke) keeps the hot path free of I/O.
    func reloadConfiguration() {
        configuration = configurationStore.load()
        inputCoordinator.apply(configuration)
        updateInputPresentation()
    }
}
