// swift-tools-version: 6.1

import PackageDescription

let package = Package(
    name: "FunputKit",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "FunputEngine", targets: ["FunputEngine"]),
        .library(name: "KeyboardInput", targets: ["KeyboardInput"]),
        .library(name: "KeyboardLayout", targets: ["KeyboardLayout"]),
        .library(name: "FunputShared", targets: ["FunputShared"]),
        .library(name: "ThemeSchema", targets: ["ThemeSchema"]),
        .library(name: "ThemeRuntime", targets: ["ThemeRuntime"]),
        .library(name: "KeyboardRenderer", targets: ["KeyboardRenderer"]),
        .library(name: "KeyboardConfiguration", targets: ["KeyboardConfiguration"]),
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
            dependencies: ["FunputEngine", "KeyboardLayout", "FunputShared"]
        ),
        .target(name: "KeyboardLayout"),
        .target(
            name: "FunputShared",
            dependencies: ["KeyboardLayout"]
        ),
        .target(name: "ThemeSchema"),
        .target(
            name: "ThemeRuntime",
            dependencies: ["ThemeSchema"]
        ),
        .target(
            name: "KeyboardRenderer",
            dependencies: ["KeyboardLayout", "ThemeSchema"]
        ),
        .target(
            name: "KeyboardConfiguration",
            dependencies: [
                "FunputShared",
                "ThemeRuntime",
                "ThemeSchema",
                "KeyboardRenderer",
                "KeyboardLayout",
            ]
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
            name: "ThemeRuntimeTests",
            dependencies: ["ThemeRuntime", "ThemeSchema"]
        ),
        .testTarget(
            name: "FunputSharedTests",
            dependencies: ["FunputShared", "KeyboardLayout"]
        ),
        .testTarget(
            name: "KeyboardConfigurationTests",
            dependencies: [
                "KeyboardConfiguration",
                "FunputShared",
                "ThemeRuntime",
                "ThemeSchema",
                "KeyboardLayout",
                "KeyboardRenderer",
            ]
        ),
        .testTarget(
            name: "FunputEngineTests",
            dependencies: ["FunputEngine"]
        ),
        .testTarget(
            name: "KeyboardInputTests",
            dependencies: ["KeyboardInput", "KeyboardLayout", "FunputShared"]
        ),
    ]
)
