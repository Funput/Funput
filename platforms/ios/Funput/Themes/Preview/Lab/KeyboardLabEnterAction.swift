import KeyboardLayout

enum KeyboardLabEnterAction: String, CaseIterable, Identifiable {
    case newLine
    case go
    case search
    case send
    case next
    case done
    case previous
    case custom

    var id: Self { self }

    var value: KeyboardEnterAction {
        switch self {
        case .newLine: .newLine
        case .go: .go
        case .search: .search
        case .send: .send
        case .next: .next
        case .done: .done
        case .previous: .previous
        case .custom: .custom("Apply")
        }
    }
}
