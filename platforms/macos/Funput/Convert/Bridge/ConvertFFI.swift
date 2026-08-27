import Foundation

nonisolated final class ConvertFFISession: @unchecked Sendable {
    private let handle: OpaquePointer

    init?() {
        guard let handle = funput_convert_session_new() else { return nil }
        self.handle = handle
        funput_convert_session_refresh(handle)
    }

    deinit { funput_convert_session_free(handle) }

    func reset() { funput_convert_session_reset(handle); refresh() }
    func refresh() { funput_convert_session_refresh(handle) }
    func setTarget(_ index: Int) { funput_convert_session_set_target(handle, UInt(index)); refresh() }
    func setSource(_ index: Int?) {
        funput_convert_session_pick_source(handle, Int32(index ?? -1)); refresh()
    }
    func setRowSource(row: Int, source: Int?) {
        funput_convert_session_pick_row_source(handle, UInt(row), Int32(source ?? -1)); refresh()
    }
    func setRowCount(_ count: Int) {
        funput_convert_session_set_row_window(handle, 0, UInt(count)); refresh()
    }
    func setInput(_ input: String) {
        let scalars = input.unicodeScalars.map(\.value)
        scalars.withUnsafeBufferPointer {
            funput_convert_session_set_input(handle, $0.baseAddress, UInt($0.count))
        }
        refresh()
    }

    func adopt(paths: [String]) -> Bool {
        guard let scan = funput_convert_scan_new() else { return false }
        defer { funput_convert_scan_free(scan) }
        for path in paths {
            let added = path.utf8CString.withUnsafeBytes { bytes in
                funput_convert_scan_add_path(scan, bytes.baseAddress?.assumingMemoryBound(to: UInt8.self), UInt(bytes.count - 1))
            }
            guard added else { return false }
        }
        return funput_convert_scan_run(scan)
            && funput_convert_session_adopt_scan(handle, scan)
            && refreshed()
    }

    func runBatch() -> String? {
        guard let job = funput_convert_job_new(handle) else { return nil }
        defer { funput_convert_job_free(job) }
        guard funput_convert_job_run(job) else { return nil }
        return readText { funput_convert_job_report(job, $0, $1) }
    }

    func resultText() -> String { readText { funput_convert_session_result_text(handle, $0, $1) } }
    func saveBytes() -> Data {
        let count = funput_convert_session_save_bytes(handle, nil, 0)
        var bytes = [UInt8](repeating: 0, count: Int(count))
        bytes.withUnsafeMutableBufferPointer {
            _ = funput_convert_session_save_bytes(handle, $0.baseAddress, UInt($0.count))
        }
        return Data(bytes)
    }

    func state(input: String, charsets: [ConvertCharset]) -> ConvertScreenState {
        let view = funput_convert_session_view(handle)
        let rows = (0..<Int(view.rows_count)).map { row(at: $0, first: Int(view.rows_first)) }
        return ConvertScreenState(
            mode: mode(view.mode), charsets: charsets, target: Int(view.target),
            source: optional(view.source), fromFile: view.from_file,
            fileName: emptyToNil(readText { funput_convert_session_file_name(handle, $0, $1) }),
            inputText: view.has_input_preview ? readText { funput_convert_session_input_preview(handle, $0, $1) } : input,
            outputText: readText { funput_convert_session_output_preview(handle, $0, $1) },
            warning: readText { funput_convert_session_warning(handle, $0, $1) }, files: rows,
            rowsTotal: Int(view.rows_total), outputDirectory: readText { funput_convert_session_out_dir(handle, $0, $1) },
            unreadable: readText { funput_convert_session_unreadable_line(handle, $0, $1) },
            progress: "", ready: Int(view.ready), isBusy: false, errorMessage: nil
        )
    }

    private func row(at index: Int, first: Int) -> ConvertFileRow {
        let row = funput_convert_session_row(handle, UInt(index))
        return .init(id: first + index,
                     name: readText { funput_convert_session_row_name(handle, UInt(index), $0, $1) },
                     source: optional(row.source),
                     note: readText { funput_convert_session_row_note(handle, UInt(index), $0, $1) })
    }

    private func refreshed() -> Bool { refresh(); return true }
}

nonisolated private func readText(_ body: (UnsafeMutablePointer<UInt32>?, UInt) -> UInt) -> String {
    let count = body(nil, 0)
    var values = [UInt32](repeating: 0, count: Int(count))
    values.withUnsafeMutableBufferPointer { _ = body($0.baseAddress, UInt($0.count)) }
    return String(values.compactMap(UnicodeScalar.init).map(Character.init))
}

nonisolated private func optional(_ value: Int32) -> Int? { value < 0 ? nil : Int(value) }
nonisolated private func emptyToNil(_ value: String) -> String? { value.isEmpty ? nil : value }
nonisolated private func mode(_ value: UInt8) -> ConvertMode {
    value == FUNPUT_CONVERT_MODE_TEXT ? .text : value == FUNPUT_CONVERT_MODE_FILES ? .files : .empty
}
