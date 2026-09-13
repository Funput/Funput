//
//  FunputApp.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

import FunputShared
import SwiftUI
import ThemeRuntime

@main
struct FunputApp: App {
    init() {
#if DEBUG
        let environment = ProcessInfo.processInfo.environment
        let harness = ProcessInfo.processInfo.arguments.contains("-uitest-typing-harness")
        let fixture = environment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] ?? (harness ? UUID().uuidString : nil)
        try? ShortcutsUITestSelection.activate(id: fixture)
        if ProcessInfo.processInfo.arguments.contains("-uitest-clear-configuration-override") {
            FunputUITestConfigurationOverrideStore().clear()
        }
#endif
        _ = KeyboardBootstrapSynchronizer().save(
            configuration: FunputConfigurationStore().load(),
            customThemes: CustomThemeStore().load()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
