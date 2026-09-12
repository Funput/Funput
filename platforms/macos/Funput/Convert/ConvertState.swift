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

/// One entry of the transform menu, named by core so three platforms cannot drift.
struct ConvertTransform: Identifiable, Equatable {
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
    var rowsTotal: Int
    var outputDirectory: String
    var unreadable: String
    var progress: String
    var ready: Int
    var isBusy: Bool
    var errorMessage: String?
    /// The menu, in core's order. A position here is the identity of a transform.
    var transforms: [ConvertTransform]
    /// What has been applied, in the order pressed — so `chữ thường → Viết Hoa Đầu
    /// Mỗi Từ` is a different document from the same two the other way round.
    var appliedTransforms: [Int]
    var keepD: Bool
    var flattenCaps: Bool

    var canUseTextResult: Bool {
        source != nil && !isBusy
    }

    var textPrimaryAction: String {
        fromFile ? "Chuyển tệp" : "Lưu tệp…"
    }

    var batchAction: String {
        isBusy ? "Đang chuyển…" : "Chuyển \(ready) tệp"
    }

    var canLoadMore: Bool {
        files.count < rowsTotal && !isBusy
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
    case loadMore
    case receiveFiles([URL])
    case casing(ConvertCasingAction)
}

typealias ConvertDispatch = (ConvertAction) -> Void

extension ConvertScreenState {
    static func empty(charsets: [ConvertCharset], transforms: [ConvertTransform]) -> Self {
        .init(
            mode: .empty, charsets: charsets, target: 0, source: nil,
            fromFile: false, fileName: nil, inputText: "", outputText: "",
            warning: "", files: [], rowsTotal: 0, outputDirectory: "",
            unreadable: "", progress: "", ready: 0, isBusy: false,
            errorMessage: nil, transforms: transforms, appliedTransforms: [],
            keepD: false, flattenCaps: false
        )
    }
}
