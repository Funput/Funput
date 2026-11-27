import SwiftUI
import Testing
import ThemeRuntime
import UIKit
@testable import Funput

/// #805: a custom theme's background photo must stay inside the card's thumbnail slot.
/// A fill-scaled photo used to size the sketch itself, so a portrait photo grew the
/// thumbnail far past its frame, stretched the keys and covered the cards around it.
@MainActor
struct ThemeCardThumbnailLayoutTests {
    private let slot = CGSize(width: 316, height: 204)
    private let canvas = CGSize(width: 900, height: 900)

    @Test("A background photo is clipped to the thumbnail slot", arguments: [
        CGSize(width: 300, height: 900),
        CGSize(width: 1200, height: 200),
    ])
    func photoStaysInsideSlot(photoSize: CGSize) throws {
        let photo = try #require(solidPhoto(size: photoSize).pngData())
        let thumbnail = ThemeCardThumbnail(
            themeID: "custom-805",
            theme: ThemeRuntime.resolve(.funputGlass),
            backgroundImageData: photo,
            interfaceStyle: .light
        )
        let scene = ZStack {
            Color.white
            thumbnail.frame(width: slot.width, height: slot.height)
        }
        .frame(width: canvas.width, height: canvas.height)

        let renderer = ImageRenderer(content: scene)
        renderer.scale = 1
        let image = try #require(renderer.cgImage)
        let frame = CGRect(
            x: (canvas.width - slot.width) / 2, y: (canvas.height - slot.height) / 2,
            width: slot.width, height: slot.height
        )

        // Just outside the slot stays the canvas color; inside, the photo shows past the keys.
        #expect(isWhite(image, at: CGPoint(x: frame.midX, y: frame.minY - 20)))
        #expect(isWhite(image, at: CGPoint(x: frame.midX, y: frame.maxY + 20)))
        #expect(isWhite(image, at: CGPoint(x: frame.minX - 20, y: frame.midY)))
        #expect(isWhite(image, at: CGPoint(x: frame.maxX + 20, y: frame.midY)))
        #expect(!isWhite(image, at: CGPoint(x: frame.minX + 4, y: frame.midY)))
    }

    @Test("A custom theme with a captured picture shows it instead of the sketch")
    func capturedPictureReplacesSketch() throws {
        let thumbnail = ThemeCardThumbnail(
            themeID: "custom-picture",
            theme: ThemeRuntime.resolve(.funputGlass),
            backgroundImageData: nil,
            snapshot: solidPhoto(size: CGSize(width: 948, height: 612)),
            interfaceStyle: .light
        )
        .frame(width: slot.width, height: slot.height)

        let renderer = ImageRenderer(content: thumbnail)
        renderer.scale = 1
        let image = try #require(renderer.cgImage)

        // The sketch draws keys across the middle; the picture is plain red there.
        let middle = pixel(image, at: CGPoint(x: slot.width / 2, y: slot.height / 2))
        #expect(middle[0] > 200 && middle[1] < 80 && middle[2] < 80)
    }

    private func solidPhoto(size: CGSize) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor.systemRed.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
    }

    private func isWhite(_ image: CGImage, at point: CGPoint) -> Bool {
        pixel(image, at: point).prefix(3).allSatisfy { $0 > 240 }
    }

    private func pixel(_ image: CGImage, at point: CGPoint) -> [UInt8] {
        let x = Int(point.x), y = Int(point.y)
        var pixel = [UInt8](repeating: 0, count: 4)
        let context = CGContext(
            data: &pixel, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )
        context?.draw(image, in: CGRect(x: -x, y: y - image.height + 1, width: image.width, height: image.height))
        return pixel
    }
}
