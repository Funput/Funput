import KeyboardLayout

/// Groups the per-role rules the pipeline applies while a contact is still recoverable.
///
/// A fast two-thumb tap rarely lifts exactly where it landed, so the pipeline needs to know
/// which roles survive a finger that drifted. What they commit is never in question — a press
/// commits the key it landed on — so these rules decide only whether it commits at all.
/// Keeping them in one value type means a new rule is an added property here instead of
/// another `KeyboardTouchPipeline.init` parameter.
public struct KeyboardTouchRecoveryPolicy: Equatable, Sendable {
    /// Roles the pipeline tracks at all; a touch-down on anything else is ignored.
    public let eligibleRoles: Set<KeyRole>
    /// Roles that still commit their landed key after the finger travelled past the tap slop.
    public let tapSlopRecoveringRoles: Set<KeyRole>
    /// Roles that still commit their landed key when the finger lifts outside the geometry.
    public let releaseOutsideRecoveringRoles: Set<KeyRole>

    public init(
        eligibleRoles: Set<KeyRole>,
        tapSlopRecoveringRoles: Set<KeyRole>,
        releaseOutsideRecoveringRoles: Set<KeyRole>
    ) {
        self.eligibleRoles = eligibleRoles
        self.tapSlopRecoveringRoles = tapSlopRecoveringRoles
        self.releaseOutsideRecoveringRoles = releaseOutsideRecoveringRoles
    }

    /// Every eligible role survives both drift cases, so a press is never lost to a finger
    /// that moved on its way up.
    public static func recoveringAll(_ roles: Set<KeyRole>) -> Self {
        Self(
            eligibleRoles: roles,
            tapSlopRecoveringRoles: roles,
            releaseOutsideRecoveringRoles: roles
        )
    }

    func isEligible(_ role: KeyRole) -> Bool {
        eligibleRoles.contains(role)
    }

    func recoversTapSlop(_ role: KeyRole) -> Bool {
        tapSlopRecoveringRoles.contains(role)
    }

    func recoversReleaseOutside(_ role: KeyRole) -> Bool {
        releaseOutsideRecoveringRoles.contains(role)
    }
}
