import Foundation

/// The second axis gets its own action namespace rather than five more cases on
/// `ConvertAction`: the store's switch grows by one line instead of five, and it
/// mirrors how the Rust and C layers keep the axis in a module of its own.
enum ConvertCasingAction: Equatable {
    case apply(Int)
    case undo
    case clear
    case setKeepD(Bool)
    case setFlattenCaps(Bool)
}

/// The two transforms that own a switch, by their fixed position in core's
/// append-only menu (`funput_convert::casing::ALL`).
enum ConvertTransformKind {
    case noDiacritics
    case title

    var position: Int {
        switch self {
        case .noDiacritics: 2
        case .title: 4
        }
    }
}

extension ConvertScreenState {
    /// Whether the transform buttons do anything yet. A document whose charset
    /// nothing could place is not converted, and core does not transform it either —
    /// one rule, not two. A batch carries a charset per row, so it is never blocked.
    var canUseCasing: Bool {
        !isBusy && (mode == .files || source != nil)
    }

    /// The applied transforms as names, for the `Đang áp:` line.
    var appliedTransformNames: [String] {
        appliedTransforms.compactMap { position in
            transforms.first { $0.id == position }?.name
        }
    }

    /// A switch is shown only while the transform it belongs to is applied: it has
    /// nothing to say otherwise, and showing it there teaches which one owns it.
    func showsSwitch(for transform: ConvertTransformKind) -> Bool {
        appliedTransforms.contains(transform.position)
    }
}
