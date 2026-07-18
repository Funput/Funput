import Foundation

enum InputEventPolicy {
    static func isBoundary(_ scalar: Unicode.Scalar, method: InputMethod) -> Bool {
        guard scalar.isASCII else { return false }
        if method == .telexAdvanced, scalar == "[" || scalar == "]" {
            return false
        }
        if scalar == " " || scalar == "\t" || scalar == "\n" || scalar == "\r" {
            return true
        }
        let value = scalar.value
        return (0x21...0x2F).contains(value) || (0x3A...0x40).contains(value)
            || (0x5B...0x60).contains(value) || (0x7B...0x7E).contains(value)
    }
}
