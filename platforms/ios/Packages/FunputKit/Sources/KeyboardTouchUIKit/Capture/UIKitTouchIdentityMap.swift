#if canImport(UIKit)
import KeyboardTouchCore
import UIKit

@MainActor
struct UIKitTouchIdentityMap {
    private var contacts: [ObjectIdentifier: ContactID] = [:]
    private var nextRawID: UInt64 = 1

    mutating func begin(_ touch: UITouch) -> (ContactID, ContactID?) {
        let objectID = ObjectIdentifier(touch)
        let stale = contacts.removeValue(forKey: objectID)
        precondition(nextRawID < UInt64.max)
        let contactID = ContactID(rawValue: nextRawID)
        nextRawID += 1
        contacts[objectID] = contactID
        return (contactID, stale)
    }

    func contactID(for touch: UITouch) -> ContactID? {
        contacts[ObjectIdentifier(touch)]
    }

    mutating func end(_ touch: UITouch) -> ContactID? {
        contacts.removeValue(forKey: ObjectIdentifier(touch))
    }

    mutating func reset() {
        contacts.removeAll(keepingCapacity: true)
    }
}
#endif
