//
//  ContentView.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

import SwiftUI

struct ContentView: View {
    @State private var showsLaunchExperience = true

    /// UI-test hook: `-uitest-typing-harness` swaps the whole app for a bare
    /// text view so automated typing runs have a stable, smart-feature-free
    /// target (see `TypingHarnessView`).
    private var showsTypingHarness: Bool {
        ProcessInfo.processInfo.arguments.contains("-uitest-typing-harness")
    }

    var body: some View {
        if showsTypingHarness {
            TypingHarnessView()
        } else {
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
}

#Preview {
    ContentView()
}
