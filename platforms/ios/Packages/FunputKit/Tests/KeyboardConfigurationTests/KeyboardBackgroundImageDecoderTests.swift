#if canImport(UIKit)
@testable import KeyboardConfiguration
import Testing
import UIKit

@MainActor
struct KeyboardBackgroundImageDecoderTests {
    @Test("A stored asset is decoded down to the requested budget")
    func downsamplesToBudget() throws {
        let data = try #require(jpeg(width: 2048, height: 1536))

        let image = try #require(KeyboardBackgroundImageDecoder.decode(data, maxPixelSize: 512))

        #expect(max(image.size.width, image.size.height) == 512)
    }

    @Test("Budgets above the source do not upscale it")
    func neverUpscales() throws {
        let data = try #require(jpeg(width: 320, height: 240))

        let image = try #require(KeyboardBackgroundImageDecoder.decode(data, maxPixelSize: 4096))

        #expect(max(image.size.width, image.size.height) == 320)
    }

    @Test("A budget of zero or less decodes nothing", arguments: [0, -1])
    func rejectsEmptyBudget(budget: Int) throws {
        let data = try #require(jpeg(width: 64, height: 64))

        #expect(KeyboardBackgroundImageDecoder.decode(data, maxPixelSize: budget) == nil)
    }

    @Test("Corrupt data decodes to nil rather than trapping")
    func rejectsCorruptData() {
        #expect(KeyboardBackgroundImageDecoder.decode(Data([0xFF, 0xD8]), maxPixelSize: 512) == nil)
    }

    /// Renders at scale 1 so the arguments are pixels, which is what the decoder's
    /// budget is measured in — the default renderer scale would make a "320pt" source
    /// a 960px JPEG on a 3x device and the expectations would depend on the simulator.
    private func jpeg(width: CGFloat, height: CGFloat) -> Data? {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        let size = CGSize(width: width, height: height)
        let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor.systemTeal.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
        return image.jpegData(compressionQuality: 0.8)
    }
}
#endif
