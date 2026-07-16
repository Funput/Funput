//
//  FunputApp.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

import FunputShared
import SwiftUI

@main
struct FunputApp: App {
    init() {
        if ProcessInfo.processInfo.arguments.contains("-uitest-clear-configuration-override") {
            FunputUITestConfigurationOverrideStore().clear()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
