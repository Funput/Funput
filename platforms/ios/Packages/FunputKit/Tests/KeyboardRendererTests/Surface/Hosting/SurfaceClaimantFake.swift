#if canImport(UIKit)
import KeyboardRenderer
import UIKit

/// A keyboard controller reduced to what the surface host asks of one.
@MainActor
final class SurfaceClaimantFake: KeyboardSurfaceClaimant {
    let surfaceContainer = UIView(frame: CGRect(x: 0, y: 0, width: 390, height: 304))
    private(set) var bound: [KeyboardSurfaceView] = []
    private(set) var surrenders = 0

    func bind(_ surface: KeyboardSurfaceView) { bound.append(surface) }
    func surrenderSurface() { surrenders += 1 }
}

/// Counts how often the host had to build a surface.
@MainActor
final class SurfaceFactorySpy {
    private(set) var built = 0

    func make() -> KeyboardSurfaceView {
        built += 1
        return KeyboardSurfaceView()
    }
}
#endif
