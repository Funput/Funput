import Foundation

/// Vietnamese input method. String raw values are stable persistence/config keys;
/// C wire values are mapped separately so adding methods never depends on enum order.
enum InputMethod: String, CaseIterable, Identifiable, Codable {
    case telex
    case vni
    case telexAdvanced = "telex_advanced"

    static let displayCases: [InputMethod] = [.telex, .telexAdvanced, .vni]

    var id: String { rawValue }

    var ffiValue: UInt8 {
        switch self {
        case .telex: UInt8(METHOD_TELEX)
        case .vni: UInt8(METHOD_VNI)
        case .telexAdvanced: UInt8(METHOD_TELEX_ADVANCED)
        }
    }

    var displayName: String {
        switch self {
        case .telex: "Telex"
        case .vni: "VNI"
        case .telexAdvanced: "Telex nâng cao"
        }
    }

    var blurb: String {
        switch self {
        case .telex: "Dấu bằng chữ cái — aa→â, ow→ơ, as→á, dd→đ"
        case .vni: "Dấu bằng chữ số — a6→â, o7→ơ, a1→á, d9→đ"
        case .telexAdvanced: "Full Telex — [→ư, ]→ơ, w đầu từ→ư"
        }
    }

    static func persisted(_ value: Any?) -> InputMethod {
        if let key = value as? String, let method = InputMethod(rawValue: key) {
            return method
        }
        guard let legacy = value as? NSNumber else { return .telex }
        switch legacy.intValue {
        case 1: return .vni
        case 2: return .telexAdvanced
        default: return .telex
        }
    }
}

/// Tone-mark placement style. Raw value matches `funput_core`
/// (`Traditional = 0`, `Modern = 1`), so it passes straight into `FunputConfig`.
enum ToneStyle: Int, CaseIterable, Identifiable, Codable {
    case traditional = 0
    case modern = 1

    var id: Int { rawValue }

    var displayName: String {
        switch self {
        case .traditional: "Truyền thống"
        case .modern: "Hiện đại"
        }
    }

    var blurb: String {
        switch self {
        case .traditional: "Dấu kiểu cũ — hòa, khỏe, thúy"
        case .modern: "Dấu kiểu mới — hoà, khoẻ, thuý"
        }
    }
}
