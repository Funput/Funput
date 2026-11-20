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

    /// Capture the complete expansion before the FFI's borrowed UTF-8 output expires.
    func processText(_ scalar: Unicode.Scalar) -> (result: FunputResult, output: String) {
        Self.receivingText { funput_process_key_text(handle, scalar.value, 0, Self.receiveText, $0) }
    }

    /// Run `call` with a fresh `String` for `receiveText` to fill, and return both. This is
    /// how every engine call that hands text back over UTF-8 is made: the text is borrowed
    /// only for the duration of the call, never through `FunputResult.chars`, which stops at
    /// `CHARS_CAP` codepoints.
    static func receivingText<Value>(_ call: (UnsafeMutableRawPointer) -> Value) -> (Value, String) {
        var text = ""
        let value = withUnsafeMutablePointer(to: &text) { call(UnsafeMutableRawPointer($0)) }
        return (value, text)
    }

    /// The engine's UTF-8 callback: copies the borrowed text into the `String` behind
    /// `context` (see `receivingText`).
    static let receiveText: @convention(c) (UnsafeMutableRawPointer?, UnsafePointer<UInt8>?, UInt) -> Void = {
        context, bytes, count in
        guard let context, let bytes else { return }
        context.assumingMemoryBound(to: String.self).pointee = String(
            decoding: UnsafeBufferPointer(start: bytes, count: Int(count)), as: UTF8.self)
    }
}
