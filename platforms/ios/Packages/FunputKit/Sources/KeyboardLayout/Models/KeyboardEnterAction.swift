import Foundation

public enum KeyboardEnterAction: Hashable, Sendable {
    case newLine
    case go
    case search
    case send
    case next
    case done
    case previous
    case custom(String)
}
