import Foundation

public protocol ShortcutsStoring: Sendable {
    func load() async throws -> ShortcutLibrary
    func save(_ library: ShortcutLibrary) async throws
}

public enum ShortcutsStorageError: Error, LocalizedError, Equatable {
    case unavailable, readFailed, writeFailed, invalidData, unsupportedVersion, duplicateTrigger

    public var errorDescription: String? {
        switch self {
        case .unavailable:
            "Không thể truy cập nơi lưu dữ liệu Gõ tắt. Vui lòng thử lại."
        case .readFailed:
            "Không thể đọc dữ liệu Gõ tắt. Dữ liệu hiện có được giữ nguyên."
        case .writeFailed:
            "Không thể lưu dữ liệu Gõ tắt. Dữ liệu đã lưu được giữ nguyên. Vui lòng thử lại."
        case .invalidData:
            "Dữ liệu Gõ tắt không hợp lệ. Tệp hiện có được giữ nguyên."
        case .unsupportedVersion:
            "Phiên bản dữ liệu Gõ tắt chưa được hỗ trợ. Hãy cập nhật Funput để mở dữ liệu này."
        case .duplicateTrigger:
            "Chữ tắt này đã có trong danh sách. Hãy sửa mục đã có hoặc chọn chữ tắt khác."
        }
    }
}
