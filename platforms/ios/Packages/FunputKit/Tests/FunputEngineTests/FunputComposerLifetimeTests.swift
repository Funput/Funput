#if os(iOS) && canImport(FunputCore)
import Foundation
@testable import FunputEngine
import Testing

private final class ReleaseSpy: @unchecked Sendable {
    private let lock = NSLock()
    private var handles: [OpaquePointer] = []

    func release(_ handle: OpaquePointer) {
        lock.withLock {
            handles.append(handle)
        }
    }

    var releasedHandles: [OpaquePointer] {
        lock.withLock { handles }
    }
}

@MainActor
struct FunputComposerLifetimeTests {
    @Test("Composer releases with its Swift owner")
    func releasesWithOwner() {
        weak var releasedComposer: FunputComposer?
        autoreleasepool {
            let composer = FunputComposer()
            composer.process("a")
            releasedComposer = composer
            #expect(releasedComposer != nil)
        }
        #expect(releasedComposer == nil)
    }

    @Test("Composer invokes its release function exactly once")
    func invokesReleaseExactlyOnce() {
        let fakeHandle = OpaquePointer(bitPattern: 0xF00D)!
        let spy = ReleaseSpy()

        autoreleasepool {
            _ = FunputComposer(handle: fakeHandle) { spy.release($0) }
        }

        #expect(spy.releasedHandles == [fakeHandle])
    }

    @Test("Repeated create-use-release cycles remain safe")
    func repeatedLifetime() {
        for _ in 0..<1_000 {
            autoreleasepool {
                let composer = FunputComposer()
                composer.setInputMethod(.vni)
                #expect(composer.process("a").action == .none)
            }
        }
    }
}
#endif
