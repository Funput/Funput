#if canImport(UIKit)
import ThemeSchema
import UIKit

extension ThemeGradientDirection {
    var layerPoints: (start: CGPoint, end: CGPoint) {
        switch self {
        case .horizontal:
            (CGPoint(x: 0, y: 0.5), CGPoint(x: 1, y: 0.5))
        case .vertical:
            (CGPoint(x: 0.5, y: 0), CGPoint(x: 0.5, y: 1))
        case .diagonalRight:
            (CGPoint(x: 0.08, y: 0), CGPoint(x: 0.92, y: 1))
        case .diagonalLeft:
            (CGPoint(x: 0.92, y: 0), CGPoint(x: 0.08, y: 1))
        }
    }
}
#endif
