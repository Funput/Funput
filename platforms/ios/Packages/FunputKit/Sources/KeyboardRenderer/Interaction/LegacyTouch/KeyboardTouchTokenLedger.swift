#if canImport(UIKit)
import UIKit

/// Maps UIKit's touch objects to commit tokens and decides when a tracked touch
/// has really been abandoned.
///
/// UIKit dispatches one `touches…` callback per phase group of an event, so a
/// finger that has just lifted already reports `.ended` while its own terminal
/// callback is still queued behind the next finger's `touchesBegan`. Reading the
/// phase alone therefore cannot tell "lifted, delivery pending" from "abandoned":
/// a touch is only given up once UIKit stops listing it, or once a *later* event
/// still reports it finished. Anything stricter cancels a real keystroke.
///
/// Touch delivery is main-thread only, and reading `UITouch.phase` requires the
/// main actor, so the whole ledger is isolated to it.
@MainActor
struct KeyboardTouchTokenLedger {
    typealias TouchToken = KeyboardPressCommitQueue.TouchToken

    private var tokens: [ObjectIdentifier: TouchToken] = [:]
    private var finishedSince: [ObjectIdentifier: TimeInterval] = [:]
    private var nextToken: TouchToken = 1

    init() {
        tokens.reserveCapacity(10)
    }

    var trackedTokens: Set<TouchToken> { Set(tokens.values) }

    func token(for touch: UITouch) -> TouchToken? {
        tokens[ObjectIdentifier(touch)]
    }

    /// Starts tracking `touch`. A mapping that survives for the same object is
    /// stale by definition — a touch object never begins twice, and UIKit recycles
    /// them — so it is handed back to be retired instead of the new press being
    /// discarded as a duplicate.
    mutating func beginToken(for touch: UITouch) -> (token: TouchToken, stale: TouchToken?) {
        let identifier = ObjectIdentifier(touch)
        let stale = tokens.removeValue(forKey: identifier)
        finishedSince.removeValue(forKey: identifier)
        let token = nextToken
        nextToken &+= 1
        tokens[identifier] = token
        return (token, stale)
    }

    mutating func removeToken(for touch: UITouch) -> TouchToken? {
        let identifier = ObjectIdentifier(touch)
        finishedSince.removeValue(forKey: identifier)
        return tokens.removeValue(forKey: identifier)
    }

    mutating func removeAllTokens() {
        tokens.removeAll(keepingCapacity: true)
        finishedSince.removeAll(keepingCapacity: true)
    }

    /// Forgets the touches UIKit has stopped reporting and returns the survivors.
    mutating func survivors(
        in allTouches: Set<UITouch>,
        at timestamp: TimeInterval
    ) -> Set<TouchToken> {
        var isFinishedByIdentifier: [ObjectIdentifier: Bool] = [:]
        isFinishedByIdentifier.reserveCapacity(allTouches.count)
        for touch in allTouches {
            isFinishedByIdentifier[ObjectIdentifier(touch)] =
                touch.phase == .ended || touch.phase == .cancelled
        }

        var abandoned: [ObjectIdentifier] = []
        for identifier in tokens.keys {
            guard let isFinished = isFinishedByIdentifier[identifier] else {
                abandoned.append(identifier) // UIKit no longer knows this touch
                continue
            }
            guard isFinished else {
                finishedSince.removeValue(forKey: identifier)
                continue
            }
            guard let since = finishedSince[identifier] else {
                finishedSince[identifier] = timestamp // terminal callback still in flight
                continue
            }
            // A later event still reports it finished, so the callback is never
            // coming; give it up rather than block the commit queue behind it.
            if timestamp > since { abandoned.append(identifier) }
        }

        for identifier in abandoned {
            tokens.removeValue(forKey: identifier)
            finishedSince.removeValue(forKey: identifier)
        }
        return Set(tokens.values)
    }
}
#endif
