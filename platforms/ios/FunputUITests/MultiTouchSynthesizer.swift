import XCTest

/// One synthesized finger: touches down at `point` at `downOffset` seconds
/// into the batch and lifts at `upOffset`. Strokes in a batch may overlap in
/// time — that is the whole point: XCUIElement.tap() is strictly sequential
/// and can never produce the rollover (next key down before previous key up)
/// that real two-thumb typing generates.
struct TouchStroke {
    let point: CGPoint
    let downOffset: TimeInterval
    let upOffset: TimeInterval
}

/// Sends true multi-touch sequences through XCTest's event-synthesis daemon
/// (`XCSynthesizedEventRecord` + `XCPointerEventPath`) — the same private API
/// WebDriverAgent uses for W3C multi-pointer actions. Test-bundle only.
enum MultiTouchSynthesizer {
    enum SynthesisError: Error, CustomStringConvertible {
        case privateAPIUnavailable(String)
        case synthesisFailed(String)

        var description: String {
            switch self {
            case .privateAPIUnavailable(let what):
                return "XCTest private event-synthesis API unavailable (\(what)) — "
                    + "the Xcode version may have renamed it"
            case .synthesisFailed(let why):
                return "event synthesis failed: \(why)"
            }
        }
    }

    /// Plays every stroke in one synthesized event record and blocks until the
    /// daemon reports completion. `point`s are absolute screen coordinates in
    /// portrait orientation (matching XCUIElement.frame).
    static func perform(_ strokes: [TouchStroke], in testCase: XCTestCase) throws {
        guard !strokes.isEmpty else { return }
        guard let recordClass = NSClassFromString("XCSynthesizedEventRecord") else {
            throw SynthesisError.privateAPIUnavailable("XCSynthesizedEventRecord")
        }
        guard let pathClass = NSClassFromString("XCPointerEventPath") else {
            throw SynthesisError.privateAPIUnavailable("XCPointerEventPath")
        }
        guard let sessionClass = NSClassFromString("XCTRunnerDaemonSession") else {
            throw SynthesisError.privateAPIUnavailable("XCTRunnerDaemonSession")
        }

        let recordType = unsafeBitCast(recordClass, to: EventRecordInit.Type.self)
        let record = recordType.init(
            name: "funput-rollover-typing",
            interfaceOrientation: UIInterfaceOrientation.portrait.rawValue
        )
        for stroke in strokes {
            let pathType = unsafeBitCast(pathClass, to: PointerEventPathInit.Type.self)
            let path = pathType.init(touchAt: stroke.point, offset: stroke.downOffset)
            unsafeBitCast(path, to: PointerEventPath.self).liftUp(atOffset: stroke.upOffset)
            unsafeBitCast(record, to: EventRecord.self).add(path)
        }

        let sessionType = unsafeBitCast(sessionClass, to: DaemonSessionClass.Type.self)
        let session = sessionType.sharedSession()
        let done = testCase.expectation(description: "event synthesis")
        var failure: String?
        unsafeBitCast(session, to: DaemonSession.self).synthesize(record) { handled, error in
            if let error {
                failure = error.localizedDescription
            } else if !handled {
                failure = "daemon did not handle the event"
            }
            done.fulfill()
        }
        let batchLength = strokes.map(\.upOffset).max() ?? 0
        testCase.wait(for: [done], timeout: batchLength + 10)
        if let failure {
            throw SynthesisError.synthesisFailed(failure)
        }
    }
}

// MARK: - Private-API shapes (resolved via the ObjC runtime, never linked)

@objc private protocol EventRecordInit {
    @objc(initWithName:interfaceOrientation:)
    init(name: String, interfaceOrientation: Int)
}

@objc private protocol EventRecord {
    @objc(addPointerEventPath:) func add(_ path: AnyObject)
}

@objc private protocol PointerEventPathInit {
    @objc(initForTouchAtPoint:offset:)
    init(touchAt point: CGPoint, offset: TimeInterval)
}

@objc private protocol PointerEventPath {
    @objc(liftUpAtOffset:) func liftUp(atOffset: TimeInterval)
}

@objc private protocol DaemonSessionClass {
    @objc(sharedSession) static func sharedSession() -> AnyObject
}

@objc private protocol DaemonSession {
    // Block signature verified against XCUIAutomation's embedded ObjC type
    // encoding (`v20@?0B8@"NSError"12`): (BOOL handled, NSError *error).
    @objc(synthesizeEvent:completion:)
    func synthesize(_ record: AnyObject, completion: @escaping (Bool, Error?) -> Void)
}
