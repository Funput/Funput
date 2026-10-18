#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

struct KeySwipeDownTrackerTests {
    private let padKey = KeySpec(id: "q", label: "q", role: .character, secondaryLabel: "1", labelPlacement: .bottomCenter)

    @Test("Only the iPad's bottom-centered keys with a hint can be pulled down")
    func eligibility() {
        #expect(KeySwipeDownTracker.isEligible(padKey))
        let phone = KeySpec(id: "q", label: "q", role: .character, secondaryLabel: "1")
        let bare = KeySpec(id: "q", label: "q", role: .character, labelPlacement: .bottomCenter)
        #expect(!KeySwipeDownTracker.isEligible(phone))
        #expect(!KeySwipeDownTracker.isEligible(bare))
        #expect(KeySwipeDownTracker(key: phone, keyHeight: 80) == nil)
    }

    @Test("The pull is measured against a share of the key's height, with a floor")
    func travel() throws {
        #expect(try #require(KeySwipeDownTracker(key: padKey, keyHeight: 80)).travel == 36)
        #expect(try #require(KeySwipeDownTracker(key: padKey, keyHeight: 30)).travel == KeySwipeDownTracker.minimumTravel)
    }

    @Test("A downward pull reports progress up to 1")
    func progress() throws {
        var tracker = try #require(KeySwipeDownTracker(key: padKey, keyHeight: 80))
        #expect(tracker.update(translation: CGPoint(x: 0, y: 10)) == nil)
        #expect(tracker.update(translation: CGPoint(x: 2, y: 18)) == 0.5)
        #expect(tracker.update(translation: CGPoint(x: 0, y: 90)) == 1)
    }

    @Test("Sideways, diagonal and upward movement is not a pull")
    func otherDirections() throws {
        var tracker = try #require(KeySwipeDownTracker(key: padKey, keyHeight: 80))
        #expect(tracker.update(translation: CGPoint(x: 40, y: 0)) == nil)
        #expect(tracker.update(translation: CGPoint(x: 30, y: 30)) == nil)
        #expect(tracker.update(translation: CGPoint(x: 0, y: -40)) == nil)
        #expect(tracker.progress == 0)
    }

    @Test("A claimed pull keeps tracking as the finger backs out, and types the hint past half")
    func claimedBackOut() throws {
        var tracker = try #require(KeySwipeDownTracker(key: padKey, keyHeight: 80))
        _ = tracker.update(translation: CGPoint(x: 0, y: 40))
        tracker.claimed = true
        #expect(tracker.typesHint)
        #expect(tracker.update(translation: CGPoint(x: 0, y: 9)) == 0.25)
        #expect(!tracker.typesHint)
    }
}
#endif
