import ObjectiveC
import os
import UIKit

/// How many keyboard controllers, and how many primary surfaces, are alive in this
/// process.
///
/// iOS builds a new `KeyboardViewController` in every host app but keeps the
/// process. Whatever outlives its host keeps memory that only comes back when the
/// extension is killed, and memory then climbs with every app switch. Counting the
/// controller alone is not enough: a surface can be held after its controller is
/// gone, and the surface is the heavy part. Both counts go into every `Footprint`
/// event, and a leak is logged as a fault.
enum KeyboardLifetimeCensus {
    enum Kind: Int, CaseIterable, Sendable {
        case controller, surface
    }

    private static let live = OSAllocatedUnfairLock(initialState: [Int](repeating: 0, count: Kind.allCases.count))
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Memory")

    static func liveCount(_ kind: Kind) -> Int { live.withLock { $0[kind.rawValue] } }

    /// Lives and dies with what it counts: a stored property of the controller, an
    /// associated object of a surface. Its `deinit` is nonisolated, hence the lock.
    final class Enrollment: Sendable {
        let kind: Kind

        init(_ kind: Kind) {
            self.kind = kind
            KeyboardLifetimeCensus.live.withLock { $0[kind.rawValue] += 1 }
        }

        deinit {
            let kind = kind
            let remaining = KeyboardLifetimeCensus.live.withLock { counts -> Int in
                counts[kind.rawValue] -= 1
                return counts[kind.rawValue]
            }
            os_signpost(
                .event, log: KeyboardLifetimeCensus.log, name: "Released",
                "kind=%{public}d remaining=%{public}d", kind.rawValue, remaining
            )
        }
    }

    /// Only its address matters, as the associated-object key.
    @MainActor private static var enrollmentKey: UInt8 = 0

    /// Counts a view for as long as it lives, without subclassing it.
    @MainActor
    static func track(_ view: UIView, as kind: Kind) {
        objc_setAssociatedObject(view, &enrollmentKey, Enrollment(kind), .OBJC_ASSOCIATION_RETAIN)
    }

    /// iOS may build the next keyboard before it lets the last one go, so two at once
    /// is normal for a moment. Two seconds after appearing, only the one on screen
    /// should be left.
    static let settleDelay: Duration = .seconds(2)

    static func reportLeakIfAny() {
        let controllers = liveCount(.controller)
        let surfaces = liveCount(.surface)
        guard controllers > 1 || surfaces > 1 else { return }
        os_log(
            .fault, log: log,
            "Keyboard leak: %{public}d controllers and %{public}d surfaces alive after settling",
            controllers, surfaces
        )
    }
}
