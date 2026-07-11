import KeyboardLayout

public struct KeyboardInputState: Equatable, Sendable {
    public let inputMethod: KeyboardInputMethod
    public let shiftState: ShiftState

    public init(inputMethod: KeyboardInputMethod, shiftState: ShiftState) {
        self.inputMethod = inputMethod
        self.shiftState = shiftState
    }
}
