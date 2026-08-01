// swift-tools-version: 6.1

import PackageDescription

let package = Package(
    name: "FunputKit",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "FunputEngine", targets: ["FunputEngine"]),
        .library(name: "PersonalSuggestions", targets: ["PersonalSuggestions"]),
        .library(name: "KeyboardInput", targets: ["KeyboardInput"]),
        .library(name: "KeyboardLayout", targets: ["KeyboardLayout"]),
        .library(name: "FunputShared", targets: ["FunputShared"]),
        .library(name: "ThemeSchema", targets: ["ThemeSchema"]),
        .library(name: "ThemeRuntime", targets: ["ThemeRuntime"]),
        .library(name: "KeyboardRenderer", targets: ["KeyboardRenderer"]),
        .library(name: "KeyboardConfiguration", targets: ["KeyboardConfiguration"]),
        .library(name: "KeyboardTouchCore", targets: ["KeyboardTouchCore"]),
        .library(name: "KeyboardTouchUIKit", targets: ["KeyboardTouchUIKit"]),
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
            name: "PersonalSuggestions",
            dependencies: [
                .target(name: "FunputCore", condition: .when(platforms: [.iOS])),
            ]
        ),
        .target(
            name: "KeyboardInput",
            dependencies: [
                .target(name: "FunputEngine"),
                .target(name: "KeyboardLayout"),
                .target(name: "FunputShared"),
            ]
        ),
        .target(name: "KeyboardLayout"),
        .target(
            name: "FunputShared",
            dependencies: [.target(name: "KeyboardLayout")]
        ),
        .target(name: "ThemeSchema"),
        .target(
            name: "ThemeRuntime",
            dependencies: [
                .target(name: "ThemeSchema"),
                .target(name: "FunputShared"),
            ]
        ),
        .target(
            name: "KeyboardRenderer",
            dependencies: [
                .target(name: "KeyboardLayout"),
                .target(name: "KeyboardTouchUIKit"),
                .target(name: "ThemeSchema"),
            ],
            resources: [.process("Resources")]
        ),
        .target(
            name: "KeyboardConfiguration",
            dependencies: [
                .target(name: "FunputShared"),
                .target(name: "ThemeRuntime"),
                .target(name: "ThemeSchema"),
                .target(name: "KeyboardRenderer"),
                .target(name: "KeyboardLayout"),
            ]
        ),
        .target(name: "KeyboardTouchCore"),
        .target(
            name: "KeyboardTouchUIKit",
            dependencies: [
                .target(name: "KeyboardLayout"),
                .target(name: "KeyboardTouchCore"),
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
            name: "PersonalSuggestionsTests",
            dependencies: ["PersonalSuggestions"]
        ),
        .testTarget(
            name: "KeyboardInputTests",
            dependencies: ["KeyboardInput", "KeyboardLayout", "FunputShared"]
        ),
        .testTarget(
            name: "KeyboardTouchCoreTests",
            dependencies: ["KeyboardTouchCore"]
        ),
        .testTarget(
            name: "KeyboardTouchUIKitTests",
            dependencies: [
                "KeyboardLayout",
                "KeyboardRenderer",
                "KeyboardTouchCore",
                "KeyboardTouchUIKit",
            ]
        ),
    ]
)
