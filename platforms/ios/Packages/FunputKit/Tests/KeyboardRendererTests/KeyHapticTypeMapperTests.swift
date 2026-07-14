#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

struct KeyHapticTypeMapperTests {
    @Test("Printable, control, delete and submit roles map semantically")
    func roleMapping() {
        #expect(KeyHapticTypeMapper.map(.character) == .keyPress)
        #expect(KeyHapticTypeMapper.map(.vniModifier) == .keyPress)
        #expect(KeyHapticTypeMapper.map(.punctuation) == .keyPress)
        #expect(KeyHapticTypeMapper.map(.space) == .space)
        #expect(KeyHapticTypeMapper.map(.shift) == .control)
        #expect(KeyHapticTypeMapper.map(.symbols) == .control)
        #expect(KeyHapticTypeMapper.map(.backspace) == .delete)
        #expect(KeyHapticTypeMapper.map(.enter) == .submit)
        #expect(KeyHapticTypeMapper.map(.placeholder) == nil)
    }
}
#endif
