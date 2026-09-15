import Foundation
import Testing

enum LexiconTestResource {
    static func url() throws -> URL {
        let plugins = try #require(Bundle.main.builtInPlugInsURL)
        let bundle = try #require(Bundle(url: plugins.appendingPathComponent("Keyboard.appex")))
        let url = try #require(bundle.url(forResource: "en", withExtension: "lex"))
        let size = try #require(url.resourceValues(forKeys: [.fileSizeKey]).fileSize)
        // 512 KiB, as build-lexicon.sh and en_lex_stays_under_its_size_ceiling enforce.
        #expect(size > 0 && size <= 512 * 1024)
        return url
    }
}
