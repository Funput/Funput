import FunputShared
import Testing

/// Time is simulated: each `sleep` advances the fake clock by exactly what was
/// asked, and `duringSleep` stands in for the user touching keys meanwhile.
@MainActor
final class IdleGateFixture {
    private let origin = ContinuousClock.now
    var elapsed: Duration = .zero
    var sleeps: [Duration] = []
    var duringSleep: ((Int) -> Void)?
    var runs = 0
    lazy var gate = ClipboardIdleGate(
        quietPeriod: .seconds(2),
        now: { [unowned self] in origin + elapsed },
        sleep: { [unowned self] duration in
            sleeps.append(duration)
            elapsed += duration
            duringSleep?(sleeps.count)
        }
    )
}

@MainActor
struct ClipboardIdleGateTests {
    @Test func runsOnceAfterTheQuietPeriod() async {
        let f = IdleGateFixture()
        f.gate.requestCapture { f.runs += 1 }
        #expect(f.gate.isWaiting)
        await f.gate.settle()
        #expect(f.runs == 1)
        #expect(f.sleeps == [.seconds(2)])
        #expect(!f.gate.isWaiting)
    }

    /// Typing keeps pushing the read back; it runs only once the user stops.
    @Test func keyActivityPostponesTheCapture() async {
        let f = IdleGateFixture()
        f.duringSleep = { count in
            if count < 3 {
                f.elapsed -= .milliseconds(500)
                f.gate.noteActivity()
                f.elapsed += .milliseconds(500)
            }
        }
        f.gate.requestCapture { f.runs += 1 }
        await f.gate.settle()
        #expect(f.runs == 1)
        #expect(f.sleeps == [.seconds(2), .milliseconds(1_500), .milliseconds(1_500)])
    }

    @Test func activityBeforeTheRequestCountsToo() async {
        let f = IdleGateFixture()
        f.elapsed = .seconds(10)
        f.gate.noteActivity()
        f.elapsed += .milliseconds(1_200)
        f.gate.requestCapture { f.runs += 1 }
        await f.gate.settle()
        #expect(f.runs == 1)
        #expect(f.sleeps == [.milliseconds(800)])
    }

    @Test func cancelDropsThePendingCapture() async {
        let f = IdleGateFixture()
        f.duringSleep = { _ in f.gate.cancel() }
        f.gate.requestCapture { f.runs += 1 }
        await f.gate.settle()
        #expect(f.runs == 0)
        #expect(!f.gate.isWaiting)
    }

    @Test func newerRequestsCoalesceIntoOneRun() async {
        let f = IdleGateFixture()
        var ran: [String] = []
        f.gate.requestCapture { ran.append("old") }
        f.gate.requestCapture { ran.append("new") }
        await f.gate.settle()
        #expect(ran == ["new"])
        #expect(f.sleeps.count == 1)
    }

    @Test func aCaptureAfterOneHasRunWaitsAgain() async {
        let f = IdleGateFixture()
        f.gate.requestCapture { f.runs += 1 }
        await f.gate.settle()
        f.gate.noteActivity()
        f.gate.requestCapture { f.runs += 1 }
        await f.gate.settle()
        #expect(f.runs == 2)
        #expect(f.sleeps.count == 2)
    }
}
