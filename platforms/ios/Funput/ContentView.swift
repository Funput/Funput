//
//  ContentView.swift
//  Funput
//
//  Created by P-Code Dynamics on 11/7/26.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            KeyboardLabView()
                .navigationTitle("Keyboard Lab")
        }
    }
}

#Preview {
    ContentView()
}
