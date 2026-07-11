import KeyboardLayout

public struct KeyboardInputState: Equatable, Sendable {
    public let inputMethod: KeyboardInputMethod
    public let shiftState: ShiftState
    public let layoutMode: KeyboardLayoutMode
    public let editorMode: KeyboardEditorMode
    public let enterAction: KeyboardEnterAction

    public init(
        inputMethod: KeyboardInputMethod,
        shiftState: ShiftState,
        layoutMode: KeyboardLayoutMode = .letters,
        editorMode: KeyboardEditorMode = .text,
        enterAction: KeyboardEnterAction = .newLine
    ) {
        self.inputMethod = inputMethod
        self.shiftState = shiftState
        self.layoutMode = layoutMode
        self.editorMode = editorMode
        self.enterAction = enterAction
    }
}
