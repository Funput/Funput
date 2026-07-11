import KeyboardLayout

extension KeyboardInputMethod {
    var title: String { rawValue.uppercased() }
}

extension KeyboardLayoutMode {
    var title: String {
        switch self {
        case .letters: "Chữ cái"
        case .symbolsPrimary: "Ký hiệu 1"
        case .symbolsSecondary: "Ký hiệu 2"
        }
    }
}

extension KeyboardEditorMode {
    var title: String {
        switch self {
        case .text: "Văn bản"
        case .search: "Tìm kiếm"
        case .email: "Email"
        case .url: "URL"
        case .phone: "Điện thoại"
        case .password: "Mật khẩu"
        case .pin: "PIN"
        case .number: "Số"
        case .numberDecimal: "Số thập phân"
        case .numberSigned: "Số có dấu"
        case .numberSignedDecimal: "Số có dấu, thập phân"
        }
    }
}

extension KeyboardLabEnterAction {
    var title: String {
        switch self {
        case .newLine: "Xuống dòng"
        case .go: "Đi"
        case .search: "Tìm kiếm"
        case .send: "Gửi"
        case .next: "Tiếp"
        case .done: "Xong"
        case .previous: "Trước"
        case .custom: "Apply (tùy chỉnh)"
        }
    }
}
