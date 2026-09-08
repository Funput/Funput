import Foundation

extension FunputComposer {
    /// Decode a `FunputResult`'s inline `chars` (a C `uint32_t[64]`, imported as a
    /// tuple) into a `String`.
    static func output(of result: FunputResult) -> String {
        var copy = result
        let count = Int(result.count)
        return withUnsafePointer(to: &copy.chars) { tuplePtr in
            tuplePtr.withMemoryRebound(to: UInt32.self, capacity: Int(CHARS_CAP)) { buf in
                var view = String.UnicodeScalarView()
                for i in 0..<count {
                    if let s = Unicode.Scalar(buf[i]) { view.append(s) }
                }
                return String(view)
            }
        }
    }

    static func scalars(_ codepoints: [UInt32], _ count: UInt) -> String {
        var view = String.UnicodeScalarView()
        for i in 0..<Int(count) {
            if let s = Unicode.Scalar(codepoints[i]) { view.append(s) }
        }
        return String(view)
    }
}
