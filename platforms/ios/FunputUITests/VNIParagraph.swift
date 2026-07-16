import XCTest

/// The shared VNI typing fixture: key sequence in, exact committed text out.
/// Reference output generated with the shared Rust engine, which is the
/// ground truth for every platform:
///   cargo run -p funput-cli -- dev run -m vni "<keys>"
enum VNIParagraph {
    static let keys =
        "ho6m nay tro7i2 trong xanh minh2 d9i dao5 quanh ho62 nho3 ro6i2 "
        + "ghe1 quan1 ca2 phe6 goi5 mo6t5 ly su7a4 d9a1 ngo6i2 nga8m1 dong2 "
        + "ngu7o7i2 qua lai5"

    static let expected =
        "hôm nay trời trong xanh mình đi dạo quanh hồ nhỏ rồi ghé quán cà "
        + "phê gọi một ly sữa đá ngồi ngắm dòng người qua lại"

    /// Pinpoints a failure instead of dumping two long strings: character
    /// counts (drops show up as a shorter actual) plus the first diverging
    /// word so the lost keystroke is obvious.
    static func diffMessage(actual: String) -> String {
        let expectedWords = expected.split(separator: " ", omittingEmptySubsequences: false)
        let actualWords = actual.split(separator: " ", omittingEmptySubsequences: false)
        var message = "committed text differs from engine reference — "
            + "\(actual.count)/\(expected.count) chars, "
            + "\(actualWords.count)/\(expectedWords.count) words"
        for (index, expectedWord) in expectedWords.enumerated() {
            let actualWord = index < actualWords.count ? actualWords[index] : ""
            if actualWord != expectedWord {
                message += "; first mismatch at word \(index + 1): "
                    + "expected \"\(expectedWord)\", got \"\(actualWord)\""
                break
            }
        }
        return message
    }
}

extension XCTestCase {
    /// Keep the full actual/expected strings in the result bundle.
    func attachText(_ string: String, name: String) {
        let attachment = XCTAttachment(string: string)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
