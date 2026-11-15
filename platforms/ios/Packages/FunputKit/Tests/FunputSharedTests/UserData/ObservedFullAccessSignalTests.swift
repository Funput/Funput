import Foundation
@testable import FunputShared
import Testing

@MainActor
@Suite("Observed Full Access signal")
struct ObservedFullAccessSignalTests {
    @Test("A posted signal reaches the observer until its token is released")
    func roundTrip() async {
        var received = 0
        var token: (any ObservedFullAccessObserving)? = ObservedFullAccessSignal.observe {
            received += 1
        }
        #expect(token != nil)

        ObservedFullAccessSignal.post()
        await waitUntil { received == 1 }
        #expect(received == 1)

        token = nil
        ObservedFullAccessSignal.post()
        try? await Task.sleep(for: .milliseconds(100))
        #expect(received == 1)
    }

    private func waitUntil(timeout: Duration = .seconds(2), _ condition: () -> Bool) async {
        let deadline = ContinuousClock.now + timeout
        while !condition(), ContinuousClock.now < deadline {
            try? await Task.sleep(for: .milliseconds(10))
        }
    }
}
