import CoreGraphics
import Foundation
import os

@MainActor
final class KeyboardLaunchTrace {
    private static let log = OSLog(
        subsystem: "app.funput.keyboard",
        category: "Launch"
    )

    private let launchID = OSSignpostID(log: log)
    private var viewDidLoadID: OSSignpostID?
    private var configurationID: OSSignpostID?
    private var openedPanels = Set<KeyboardSurface>()
    private var didRecordFirstLayout = false
    private var didFinish = false

    init() {
        os_signpost(.begin, log: Self.log, name: "ColdStart", signpostID: launchID)
    }

    static func makePanel<Panel>(
        _ surface: KeyboardSurface,
        _ make: @autoclosure () -> Panel
    ) -> Panel {
        let identifier = OSSignpostID(log: log)
        os_signpost(
            .begin, log: log, name: "PanelCreation", signpostID: identifier,
            "surface=%{public}@", surface.rawValue
        )
        let panel = make()
        os_signpost(
            .end, log: log, name: "PanelCreation", signpostID: identifier,
            "surface=%{public}@", surface.rawValue
        )
        os_signpost(
            .event, log: log, name: "PanelCreated",
            "surface=%{public}@", surface.rawValue
        )
        return panel
    }

    func beginViewDidLoad() {
        viewDidLoadID = begin("ViewDidLoad")
    }

    func endViewDidLoad() {
        end("ViewDidLoad", identifier: &viewDidLoadID)
    }

    func beginConfigurationLoad() {
        configurationID = begin("ConfigurationLoad")
    }

    func endConfigurationLoad() {
        end("ConfigurationLoad", identifier: &configurationID)
    }

    func measure<Value>(
        _ name: StaticString,
        operation: () throws -> Value
    ) rethrows -> Value {
        var identifier: OSSignpostID? = begin(name)
        defer { end(name, identifier: &identifier) }
        return try operation()
    }

    func recordHeightReady() {
        os_signpost(.event, log: Self.log, name: "HeightReady")
    }

    func recordPrimarySurfaceCreated() {
        os_signpost(.event, log: Self.log, name: "PrimarySurfaceCreated")
    }

    func recordBootstrapSnapshot(hit: Bool) {
        os_signpost(
            .event, log: Self.log, name: "BootstrapSnapshot",
            "result=%{public}@", hit ? "hit" : "fallback"
        )
    }

    func recordCustomThemeCatalogLoad() {
        os_signpost(.event, log: Self.log, name: "CustomThemeCatalogLoad")
    }

    func recordFirstLayout(size: CGSize) {
        guard !didRecordFirstLayout, size.width > 0, size.height > 0 else { return }
        didRecordFirstLayout = true
        os_signpost(
            .event, log: Self.log, name: "FirstNonZeroLayout",
            "width=%{public}.1f height=%{public}.1f",
            Double(size.width), Double(size.height)
        )
    }

    func recordPanelFirstOpen(_ surface: KeyboardSurface) {
        guard openedPanels.insert(surface).inserted else { return }
        os_signpost(
            .event, log: Self.log, name: "PanelFirstOpen",
            "surface=%{public}@", surface.rawValue
        )
    }

    func recordPanelReleased(_ surface: KeyboardSurface) {
        os_signpost(
            .event, log: Self.log, name: "PanelReleased",
            "surface=%{public}@", surface.rawValue
        )
    }

    func finish() {
        guard !didFinish else { return }
        didFinish = true
        os_signpost(.end, log: Self.log, name: "ColdStart", signpostID: launchID)
    }

    private func begin(_ name: StaticString) -> OSSignpostID {
        let identifier = OSSignpostID(log: Self.log)
        os_signpost(.begin, log: Self.log, name: name, signpostID: identifier)
        return identifier
    }

    private func end(_ name: StaticString, identifier: inout OSSignpostID?) {
        guard let value = identifier else { return }
        os_signpost(.end, log: Self.log, name: name, signpostID: value)
        identifier = nil
    }
}
