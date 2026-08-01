import Testing
import UIKit
@testable import Funput

@MainActor
struct ThemeImageProcessorTests {
    @Test("Processor downsamples, normalizes, and produces a blurred derivative")
    func processing() async throws {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 2600, height: 1300))
        let input = renderer.jpegData(withCompressionQuality: 1) { context in
            UIColor.systemPink.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 2600, height: 1300))
        }

        let result = try #require(await ThemeImageProcessor.process(input, blurRadius: 12))
        let source = try #require(UIImage(data: result.source))
        let blurred = try #require(UIImage(data: result.rendered))

        #expect(max(source.size.width, source.size.height) <= 2048)
        #expect(source.size == blurred.size)
        #expect(!result.source.isEmpty)
        #expect(!result.rendered.isEmpty)
    }
}
