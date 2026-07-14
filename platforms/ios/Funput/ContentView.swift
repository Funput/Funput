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
