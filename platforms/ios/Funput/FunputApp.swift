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
