#if canImport(FunputCore)
import FunputCore

enum FunputResultDecoder {
    static func decode(_ result: FunputResult) -> FunputCompositionResult {
        FunputCompositionResult(
            action: action(result.action),
            deleteCount: Int(result.backspace),
            text: output(from: result)
        )
    }

    static func output(from result: FunputResult) -> String {
        var copy = result
        let count = min(Int(result.count), Int(CHARS_CAP))
        return withUnsafePointer(to: &copy.chars) { tuple in
            tuple.withMemoryRebound(to: UInt32.self, capacity: Int(CHARS_CAP)) { buffer in
                string(from: buffer, count: count)
            }
        }
    }

    static func string(from codepoints: [UInt32], count: Int) -> String {
        codepoints.withUnsafeBufferPointer {
            string(from: $0.baseAddress, count: min(count, $0.count))
        }
    }

    private static func action(_ rawValue: UInt8) -> FunputCompositionAction {
        FunputCompositionAction(rawValue: rawValue) ?? .none
    }

    private static func string(
        from codepoints: UnsafePointer<UInt32>?,
        count: Int
    ) -> String {
        guard let codepoints, count > 0 else { return "" }
        var scalars = String.UnicodeScalarView()
        for index in 0..<count {
            if let scalar = Unicode.Scalar(codepoints[index]) {
                scalars.append(scalar)
            }
        }
        return String(scalars)
    }
}
#endif
