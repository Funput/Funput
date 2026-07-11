// swift-tools-version: 6.1

import PackageDescription

let package = Package(
    name: "FunputKit",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "FunputEngine", targets: ["FunputEngine"]),
        .library(name: "KeyboardInput", targets: ["KeyboardInput"]),
        .library(name: "KeyboardLayout", targets: ["KeyboardLayout"]),
        .library(name: "ThemeSchema", targets: ["ThemeSchema"]),
        .library(name: "KeyboardRenderer", targets: ["KeyboardRenderer"]),
    ],
    targets: [
        .binaryTarget(
            name: "FunputCore",
            path: "../../Frameworks/FunputCore.xcframework"
        ),
        .target(
            name: "FunputEngine",
            dependencies: [
                .target(name: "FunputCore", condition: .when(platforms: [.iOS])),
            ]
        ),
        .target(
            name: "KeyboardInput",
            dependencies: ["FunputEngine", "KeyboardLayout"]
        ),
        .target(name: "KeyboardLayout"),
        .target(name: "ThemeSchema"),
        .target(
            name: "KeyboardRenderer",
            dependencies: ["KeyboardLayout", "ThemeSchema"]
        ),
        .testTarget(
            name: "KeyboardLayoutTests",
            dependencies: ["KeyboardLayout"]
        ),
        .testTarget(
            name: "KeyboardRendererTests",
            dependencies: ["KeyboardLayout", "KeyboardRenderer", "ThemeSchema"]
        ),
        .testTarget(
            name: "FunputEngineTests",
            dependencies: ["FunputEngine"]
        ),
        .testTarget(
            name: "KeyboardInputTests",
            dependencies: ["KeyboardInput", "KeyboardLayout"]
        ),
    ]
)
