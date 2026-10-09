#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

public extension KeyboardInputCoordinator {
    /// Points a panel's field at the rules the document uses right now: the saved
    /// options, the method the Telex/VNI key last chose and the language switch.
    ///
    /// The host field's editor mode is deliberately ignored — searching emoji from a URL
    /// or email field still composes Vietnamese.
    func configure(_ field: LocalTextComposer) {
        field.configure(
            compositionOptions,
            inputMethod: state.inputMethod.engineMethod,
            composesVietnamese: state.language == .vietnamese
        )
    }
}
#endif
