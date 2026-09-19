#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

extension KeyboardInputCoordinator {
    func input(_ text: String, builder: inout InputTransactionBuilder) {
        var context = documentSynchronizer.snapshot?.contextBeforeInput
        for scalar in text.unicodeScalars {
            if usesEngine {
                // One key, one touch. The engine spends the report on the next
                // keystroke it sees, so a key that produces more than one scalar
                // describes only its first — and the engine, finding a key with no
                // evidence, leaves the whole word alone rather than guessing.
                if let touch = pendingTouch {
                    composer.setNextKeyTouch(touch)
                    pendingTouch = nil
                }
                let previous = composer.buffer()
                let signpostID = KeyboardInputSignposts.begin("ComposerFFI")
                let result = composer.process(scalar)
                KeyboardInputSignposts.end("ComposerFFI", signpostID)
                if result.action == .none {
                    insert(String(scalar), context: &context, builder: &builder)
                } else if let count = KeyboardReplacement.deletionCount(
                    scalars: result.deleteCount, buffer: previous, context: context
                ) {
                    builder.deleteBackward(count: count)
                    if let current = context { context = String(current.dropLast(count)) }
                    insert(result.text, context: &context, builder: &builder)
                } else {
                    abandonUnsafeReplacement()
                    insert(String(scalar), context: &context, builder: &builder)
                }
            } else {
                insert(String(scalar), context: &context, builder: &builder)
            }
            trackShortcutInput(scalar)
        }
        // The boundary key, if this was one, has parked its answer by now — and the
        // builder is still open, which is the only place the repair can ride along.
        applyPendingCorrection(context: &context, builder: &builder)
    }

    private func insert(_ text: String, context: inout String?, builder: inout InputTransactionBuilder) {
        builder.insert(text)
        context = (context ?? "") + text
    }

    func characterText(for key: KeySpec) -> String {
        guard state.shiftState.isUppercase else { return key.label }
        return key.shiftedLabel ?? key.label.uppercased()
    }
}
#endif
