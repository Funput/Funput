import Foundation

actor ConvertWorker {
    private let session: ConvertFFISession
    let charsets: [ConvertCharset]
    let transforms: [ConvertTransform]

    init?() {
        guard let session = ConvertFFISession() else { return nil }
        self.session = session
        charsets = Self.loadCharsets()
        transforms = Self.loadTransforms()
    }

    func current(input: String) -> ConvertScreenState {
        session.state(input: input, charsets: charsets, transforms: transforms)
    }
    func reset() -> ConvertScreenState { session.reset(); return current(input: "") }
    func setInput(_ text: String) -> ConvertScreenState { session.setInput(text); return current(input: text) }
    func setTarget(_ value: Int, input: String) -> ConvertScreenState {
        session.setTarget(value); return current(input: input)
    }
    func setSource(_ value: Int?, input: String) -> ConvertScreenState {
        session.setSource(value); return current(input: input)
    }
    func setRowSource(row: Int, source: Int, input: String) -> ConvertScreenState {
        session.setRowSource(row: row, source: source); return current(input: input)
    }
    func loadRows(_ count: Int, input: String) -> ConvertScreenState {
        session.setRowCount(count); return current(input: input)
    }
    func scan(_ urls: [URL]) -> ConvertScreenState? {
        guard session.adopt(paths: urls.map(\.path)) else { return nil }
        return current(input: "")
    }
    func casing(_ action: ConvertCasingAction, input: String) -> ConvertScreenState {
        session.apply(action); return current(input: input)
    }
    func resultText() -> String { session.resultText() }
    func saveBytes() -> Data { session.saveBytes() }
    func runBatch(input: String) -> (ConvertScreenState, String)? {
        guard let report = session.runBatch() else { return nil }
        return (current(input: input), report)
    }

    /// Both menus are read the same way — a count, then a name per index — so they
    /// share `readText` rather than spelling the two-call dance out twice.
    nonisolated static func loadCharsets() -> [ConvertCharset] {
        (0..<Int(funput_charset_count())).map { index in
            ConvertCharset(id: index, name: readText { funput_charset_name(UInt(index), $0, $1) })
        }
    }

    nonisolated static func loadTransforms() -> [ConvertTransform] {
        (0..<Int(funput_convert_transform_count())).map { index in
            ConvertTransform(
                id: index, name: readText { funput_convert_transform_name(UInt(index), $0, $1) }
            )
        }
    }
}
