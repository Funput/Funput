import CoreImage
import Foundation
import ImageIO
import UIKit

struct ProcessedThemeImage: Sendable {
    let source: Data
    let rendered: Data
}

enum ThemeImageProcessor {
    static func process(_ data: Data, blurRadius: Double) async -> ProcessedThemeImage? {
        await Task.detached(priority: .userInitiated) {
            guard let sourceImage = downsample(data),
                  let source = sourceImage.jpegData(compressionQuality: 0.82),
                  let rendered = blurredJPEG(sourceImage, radius: blurRadius)
            else { return nil }
            return ProcessedThemeImage(source: source, rendered: rendered)
        }.value
    }

    private nonisolated static func downsample(_ data: Data) -> UIImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: 2048,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
        else { return nil }
        return UIImage(cgImage: image)
    }

    private nonisolated static func blurredJPEG(_ image: UIImage, radius: Double) -> Data? {
        guard radius > 0, let input = CIImage(image: image) else {
            return image.jpegData(compressionQuality: 0.82)
        }
        let clamped = input.clampedToExtent()
        let output = clamped.applyingGaussianBlur(sigma: radius * 3).cropped(to: input.extent)
        let context = CIContext(options: [.cacheIntermediates: false])
        guard let cgImage = context.createCGImage(output, from: input.extent) else { return nil }
        return UIImage(cgImage: cgImage).jpegData(compressionQuality: 0.82)
    }
}
