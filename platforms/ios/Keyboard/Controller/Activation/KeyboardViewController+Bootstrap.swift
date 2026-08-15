import KeyboardConfiguration
import KeyboardInput
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    /// Builds the keyboard while the system is still preparing the extension.
    ///
    /// `viewWillAppear` runs inside the presentation animation, so everything resolved
    /// there is resolved too late. Two things went wrong when the whole activation
    /// lived there: the work itself (snapshot read, theme resolve, surface creation)
    /// sat on the animation's critical path, and until the height constraint went live
    /// at the end of it Auto Layout stretched the view to the full host container —
    /// measured at 956pt for the first ~500ms of a cold start, then collapsing in two
    /// steps to 254pt. Everything that does not need the document is settled here.
    ///
    /// The document half stays in `viewWillAppear`: the proxy is only bound once the
    /// host connects, and `textWillChange` — which lands before `viewWillAppear` —
    /// corrects the layout well before the keyboard becomes visible.
    func bootstrapKeyboard() {
        let source = launchTrace.measure("Bootstrap") {
            loadActivationSource(hasFullAccess: hasFullAccess)
        }
        // The layout depends on the input method, so the engine has to know the
        // configuration before the first presentation is built, or bootstrap would
        // size the keyboard for the wrong one and `viewWillAppear` would resize it.
        inputCoordinator.apply(source.configuration)
        applyTextInputTraits(force: true)
        adopt(source)
        activatePreferredHeightForBootstrap()
    }
}
