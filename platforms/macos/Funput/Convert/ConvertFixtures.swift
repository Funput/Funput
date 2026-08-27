import Foundation

enum ConvertFixtures {
    static let charsets = [
        ConvertCharset(id: 0, name: "Unicode"),
        ConvertCharset(id: 1, name: "TCVN3 (ABC)"),
        ConvertCharset(id: 2, name: "VNI-Windows"),
        ConvertCharset(id: 3, name: "Unicode tổ hợp"),
    ]

    static let empty = state(mode: .empty)

    static let pasted = state(
        mode: .text,
        source: 2,
        input: "Toâi yeâu tieáng Vieät. Haø Noäi ngaøy möa.",
        output: "Tôi yêu tiếng Việt. Hà Nội ngày mưa."
    )

    static let singleFile = state(
        mode: .text,
        target: 2,
        source: 0,
        fromFile: true,
        fileName: "tai-lieu-cu.txt",
        input: "Dữ liệu lưu trữ có ký tự € và emoji 🎉.",
        output: "Döõ lieäu löu tröõ coù kyù töï ? vaø emoji ?.",
        warning: "VNI-Windows không biểu diễn được: €, 🎉. Các ký tự này sẽ mất."
    )

    static let batch = state(
        mode: .files,
        files: [
            .init(id: 0, name: "bao-cao-1998.txt", source: 1, note: ""),
            .init(id: 1, name: "danh-sach-vni.txt", source: 2, note: "2 chữ sẽ mất"),
            .init(id: 2, name: "ghi-chu.txt", source: nil, note: "Cần chọn bảng mã"),
            .init(id: 3, name: "hop-dong.txt", source: 0, note: ""),
        ],
        outputDirectory: "Đã chuyển mã — cạnh các tệp nguồn",
        ready: 3
    )

    static var busyBatch: ConvertScreenState {
        var value = batch
        value.isBusy = true
        value.progress = "Đang chuyển 3 tệp…"
        return value
    }

    private static func state(
        mode: ConvertMode, target: Int = 0, source: Int? = nil, fromFile: Bool = false,
        fileName: String? = nil, input: String = "", output: String = "",
        warning: String = "", files: [ConvertFileRow] = [],
        outputDirectory: String = "", ready: Int = 0
    ) -> ConvertScreenState {
        ConvertScreenState(
            mode: mode, charsets: charsets, target: target, source: source,
            fromFile: fromFile, fileName: fileName, inputText: input,
            outputText: output, warning: warning, files: files,
            rowsTotal: files.count, outputDirectory: outputDirectory,
            unreadable: "", progress: "", ready: ready,
            isBusy: false, errorMessage: nil
        )
    }
}
