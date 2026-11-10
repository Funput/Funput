#if canImport(UIKit)
extension KeyboardSurfaceInteractionController {
    enum GestureClaim: Equatable {
        case alternate
        case repeatKey
        case swipe
        case trackpad
        case wordDelete

        /// Whether winning this claim takes the contact out of the pipeline entirely.
        /// Detached contacts produce no key on release, which is what a gesture that has
        /// already written to the document needs.
        var detachesContact: Bool {
            switch self {
            case .repeatKey, .trackpad, .wordDelete: true
            case .alternate, .swipe: false
            }
        }
    }
}
#endif
