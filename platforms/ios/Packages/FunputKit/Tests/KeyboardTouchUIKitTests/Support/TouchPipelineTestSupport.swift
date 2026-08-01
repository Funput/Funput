#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import UIKit

@MainActor
struct PipelineFixture {
    let pipeline: KeyboardTouchPipeline
    let clock: TouchTestClock
    let emissions: EmissionBox

    @discardableResult
    func consume(_ sample: ContactSample) -> KeyboardTouchDisposition {
        clock.now = sample.timestamp
        return pipeline.consume(sample)
    }
}

final class EmissionBox {
    var keys: [String] = []
}

@MainActor
func makeTouchPipeline(
    policy: KeyboardTouchRecoveryPolicy = .recoveringAll(
        [.character, .vniModifier, .punctuation]
    )
) -> PipelineFixture {
    let clock = TouchTestClock()
    let emissions = EmissionBox()
    let pipeline = KeyboardTouchPipeline(
        policy: policy,
        clock: { clock.now },
        schedule: { delay, action in clock.schedule(delay: delay, action: action) },
        onEmit: { emissions.keys.append($0.payload.hit.key.id) }
    )
    pipeline.updateGeometry(touchGeometry().0)
    return PipelineFixture(pipeline: pipeline, clock: clock, emissions: emissions)
}

@MainActor
final class TouchTestClock {
    struct Job {
        let id: UInt64
        let deadline: TimeInterval
        let action: @MainActor () -> Void
    }

    var now: TimeInterval = 0
    private var nextID: UInt64 = 1
    private var jobs: [Job] = []

    func schedule(delay: TimeInterval, action: @escaping @MainActor () -> Void) -> ScheduledDeadline {
        let id = nextID
        nextID += 1
        jobs.append(Job(id: id, deadline: now + delay, action: action))
        return ScheduledDeadline { [weak self] in
            self?.jobs.removeAll { $0.id == id }
        }
    }

    func advance(to timestamp: TimeInterval) {
        now = timestamp
        while let index = jobs.indices.min(by: { jobs[$0].deadline < jobs[$1].deadline }),
              jobs[index].deadline <= now {
            let job = jobs.remove(at: index)
            job.action()
        }
    }
}

func touchGeometry() -> (ResolvedKeyboard, KeySpec, KeySpec) {
    let a = KeySpec(id: "a", label: "a", role: .character)
    let b = KeySpec(id: "b", label: "b", role: .character)
    return (
        ResolvedKeyboard(
            size: CGSize(width: 100, height: 50),
            toolbarFrame: nil,
            rows: [[
                ResolvedKey(spec: a, frame: CGRect(x: 0, y: 0, width: 45, height: 50)),
                ResolvedKey(spec: b, frame: CGRect(x: 55, y: 0, width: 45, height: 50)),
            ]]
        ),
        a,
        b
    )
}

func touchSample(
    _ id: UInt64,
    _ phase: ContactPhase,
    _ timestamp: TimeInterval,
    _ point: CGPoint
) -> ContactSample {
    ContactSample(
        id: ContactID(rawValue: id),
        phase: phase,
        timestamp: timestamp,
        location: point,
        previousLocation: point
    )
}
#endif
