#if canImport(UIKit)
import UIKit

enum ThemeImageCrop {
    static func contentsRect(
        imageSize: CGSize,
        targetSize: CGSize,
        focalX: Double,
        focalY: Double,
        zoom: Double
    ) -> CGRect {
        guard imageSize.width > 0, imageSize.height > 0,
              targetSize.width > 0, targetSize.height > 0 else { return CGRect(x: 0, y: 0, width: 1, height: 1) }
        let imageAspect = imageSize.width / imageSize.height
        let targetAspect = targetSize.width / targetSize.height
        var width: CGFloat = 1
        var height: CGFloat = 1
        if imageAspect > targetAspect { width = targetAspect / imageAspect }
        else { height = imageAspect / targetAspect }
        let safeZoom = CGFloat(min(max(zoom, 1), 4))
        width /= safeZoom
        height /= safeZoom
        let centerX = CGFloat(min(max(focalX, 0), 1))
        let centerY = CGFloat(min(max(focalY, 0), 1))
        let x = min(max(centerX - width / 2, 0), 1 - width)
        let y = min(max(centerY - height / 2, 0), 1 - height)
        return CGRect(x: x, y: y, width: width, height: height)
    }
}
#endif
