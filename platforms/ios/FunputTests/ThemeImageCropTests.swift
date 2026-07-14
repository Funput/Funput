@testable import KeyboardRenderer
import Testing
import UIKit

struct ThemeImageCropTests {
    @Test(
        "Crop rect remains inside the image for representative keyboard shapes",
        arguments: [
            CGSize(width: 390, height: 304),
            CGSize(width: 844, height: 236),
            CGSize(width: 744, height: 324),
        ]
    )
    func cropBounds(_ target: CGSize) {
        for focalX in [0.0, 0.5, 1.0] {
            for focalY in [0.0, 0.5, 1.0] {
                let rect = ThemeImageCrop.contentsRect(
                    imageSize: CGSize(width: 1200, height: 1800),
                    targetSize: target,
                    focalX: focalX,
                    focalY: focalY,
                    zoom: 4
                )
                #expect(rect.minX >= 0)
                #expect(rect.minY >= 0)
                #expect(rect.maxX <= 1)
                #expect(rect.maxY <= 1)
                #expect(rect.width > 0 && rect.height > 0)
            }
        }
    }
}
