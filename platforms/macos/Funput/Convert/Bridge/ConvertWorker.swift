import Foundation

actor ConvertWorker {
    private let session: ConvertFFISession
    let charsets: [ConvertCharset]

    init?() {
        guard let session = ConvertFFISession() else { return nil }
        self.session = session
        charsets = Self.loadCharsets()
    }

    func current(input: String) -> ConvertScreenState { session.state(input: input, charsets: charsets) }
    func reset() -> ConvertScreenState { session.reset(); return current(input: "") }
    func setInput(_ text: String) -> ConvertScreenState { session.setInput(text); return current(input: text) }
    func setTarget(_ value: Int, input: String) -> ConvertScreenState {
        session.setTarget(value); return current(input: input)
    }
    func setSource(_ value: Int?, input: String) -> ConvertScreenState {
        session.setSource(value); return current(input: input)
    }
    func setRowSource(row: Int, source: Int?, input: String) -> ConvertScreenState {
        session.setRowSource(row: row, source: source); return current(input: input)
    }
    func loadRows(_ count: Int, input: String) -> ConvertScreenState {
        session.setRowCount(count); return current(input: input)
    }
    func scan(_ urls: [URL]) -> ConvertScreenState? {
        guard session.adopt(paths: urls.map(\.path)) else { return nil }
        return current(input: "")
    }
    func resultText() -> String { session.resultText() }
    func saveBytes() -> Data { session.saveBytes() }
    func runBatch(input: String) -> (ConvertScreenState, String)? {
        guard let report = session.runBatch() else { return nil }
        return (current(input: input), report)
    }

    nonisolated static func loadCharsets() -> [ConvertCharset] {
        (0..<Int(funput_charset_count())).map { index in
            let name = readCharsetName(index)
            return ConvertCharset(id: index, name: name)
        }
    }
}

nonisolated private func readCharsetName(_ index: Int) -> String {
    let count = funput_charset_name(UInt(index), nil, 0)
    var values = [UInt32](repeating: 0, count: Int(count))
    values.withUnsafeMutableBufferPointer { _ = funput_charset_name(UInt(index), $0.baseAddress, UInt($0.count)) }
    return String(values.compactMap(UnicodeScalar.init).map(Character.init))
}
