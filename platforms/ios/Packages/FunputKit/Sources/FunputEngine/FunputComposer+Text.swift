#if os(iOS) && canImport(FunputCore)
import FunputCore

extension FunputComposer {
    /// The C ABI reports the number copied, not the required capacity.
    public func buffer() -> String {
        var capacity = Int(CHARS_CAP)
        while true {
            var scalars = [UInt32](repeating: 0, count: capacity)
            let count = scalars.withUnsafeMutableBufferPointer {
                Int(funput_buffer(handle, $0.baseAddress, UInt($0.count)))
            }
            if count < capacity { return FunputResultDecoder.string(from: scalars, count: count) }
            capacity *= 2
        }
    }

    /// Copies borrowed UTF-8 during the synchronous callback, before Rust releases it.
    @discardableResult
    public func process(_ scalar: Unicode.Scalar) -> FunputCompositionResult {
        var output = ""
        let result = withUnsafeMutablePointer(to: &output) { pointer in
            funput_process_key_text(handle, scalar.value, UInt32(SOURCE_STANDARD), { context, bytes, count in
                guard let context, let bytes else { return }
                context.assumingMemoryBound(to: String.self).pointee = String(
                    decoding: UnsafeBufferPointer(start: bytes, count: Int(count)), as: UTF8.self
                )
            }, pointer)
        }
        return FunputCompositionResult(
            action: FunputCompositionAction(rawValue: result.action) ?? .none,
            deleteCount: Int(result.backspace),
            text: output
        )
    }
}
#endif
