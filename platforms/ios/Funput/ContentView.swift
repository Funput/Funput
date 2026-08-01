//
//  ContentView.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

import SwiftUI

struct ContentView: View {
    @State private var showsLaunchExperience = true

    var body: some View {
#if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-device-touch-acceptance") {
            KeyboardTouchAcceptanceView()
        } else if ProcessInfo.processInfo.arguments.contains("-uitest-typing-harness") {
            TypingHarnessView()
        } else {
            appContent
        }
#else
        appContent
#endif
    }

    private var appContent: some View {
        ZStack {
            AppShellView()
            if showsLaunchExperience {
                FunputLaunchExperience {
                    showsLaunchExperience = false
                }
                .transition(.opacity)
                .zIndex(1)
            }
        }
    }
}

#Preview {
    ContentView()
}
