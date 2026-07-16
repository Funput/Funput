//
//  FunputApp.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

#if DEBUG
import FunputShared
#endif
import SwiftUI

@main
struct FunputApp: App {
    init() {
#if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-uitest-clear-configuration-override") {
            FunputUITestConfigurationOverrideStore().clear()
        }
#endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
