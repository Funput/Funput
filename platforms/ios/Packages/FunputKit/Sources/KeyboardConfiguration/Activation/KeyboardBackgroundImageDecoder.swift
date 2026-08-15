#if canImport(UIKit)
import CoreGraphics
import Foundation
import ImageIO
import UIKit

/// Decodes a theme backdrop at the size the keyboard actually draws it.
///
/// The containing app stores backdrops with a 2048px long edge, which decodes to more
/// than 20MB of bitmap — a large share of the few dozen megabytes a keyboard extension
/// is allowed before the system kills it, and the reason a themed keyboard could come
/// back as an empty container. `UIImage(data:)` also defers the decode to the first
/// draw, putting it on the main thread mid-animation; a thumbnail decode with
/// `kCGImageSourceShouldCacheImmediately` pays that cost here instead, on a bitmap
/// small enough not to matter.
public enum KeyboardBackgroundImageDecoder {
    public static func decode(_ data: Data, maxPixelSize: Int) -> UIImage? {
        guard maxPixelSize > 0,
              let source = CGImageSourceCreateWithData(data as CFData, nil)
        else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(
            source,
            0,
            options as CFDictionary
        ) else { return nil }
        return UIImage(cgImage: image)
    }
}
#endif
