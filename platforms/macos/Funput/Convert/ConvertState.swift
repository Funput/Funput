import Foundation

enum ConvertMode: Equatable {
    case empty
    case text
    case files
}

struct ConvertCharset: Identifiable, Equatable {
    let id: Int
    let name: String
}

struct ConvertFileRow: Identifiable, Equatable {
    let id: Int
    let name: String
    var source: Int?
    var note: String
}

struct ConvertScreenState: Equatable {
    var mode: ConvertMode
    var charsets: [ConvertCharset]
    var target: Int
    var source: Int?
    var fromFile: Bool
    var fileName: String?
    var inputText: String
    var outputText: String
    var warning: String
    var files: [ConvertFileRow]
    var outputDirectory: String
    var progress: String
    var ready: Int
    var isBusy: Bool

    var canUseTextResult: Bool {
        source != nil && !isBusy
    }

    var textPrimaryAction: String {
        fromFile ? "Chuyển tệp" : "Lưu tệp…"
    }

    var batchAction: String {
        isBusy ? "Đang chuyển…" : "Chuyển \(ready) tệp"
    }
}

enum ConvertAction: Equatable {
    case paste
    case pickFiles
    case restart
    case setInput(String)
    case setSource(Int?)
    case setTarget(Int)
    case setRowSource(id: Int, source: Int?)
    case copyResult
    case saveResult
    case convertFiles
}

typealias ConvertDispatch = (ConvertAction) -> Void
