import Foundation
import PersonalDictionary

/// The word being typed into the add sheet, and what is wrong with it so far.
///
/// These are only the checks worth showing while typing. The engine has the last
/// word when the draft is saved, under the same rules the keyboard applies.
struct DictionaryWordDraft: Equatable {
    var text = ""

    enum Problem: Equatable {
        case spaced
        case tooLong
    }

    var word: String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var problem: Problem? {
        if word.contains(where: \.isWhitespace) { return .spaced }
        if word.unicodeScalars.count > DictionaryLimits.maxWordLength { return .tooLong }
        return nil
    }

    var canSave: Bool {
        !word.isEmpty && problem == nil
    }

    /// The spelling the bar will offer: the store keeps words lowercase.
    var suggestionPreview: String {
        word.lowercased()
    }
}

extension DictionaryWordDraft.Problem {
    var message: String {
        switch self {
        case .spaced: "Mỗi lần thêm một từ, không có dấu cách. Muốn thêm cả cụm, hãy dùng Gõ tắt."
        case .tooLong: "Từ dài quá \(DictionaryLimits.maxWordLength) ký tự."
        }
    }
}

extension DictionaryEditOutcome {
    /// Why a save did not close the sheet, or `nil` when it should close.
    var addMessage: String? {
        switch self {
        case .applied: nil
        case .unchanged: "Từ này đã có trong từ điển."
        case .limitReached: "Đã đủ số từ tự thêm tối đa. Hãy xoá bớt rồi thêm lại."
        case .invalid: "Funput không lưu được từ này."
        }
    }
}
