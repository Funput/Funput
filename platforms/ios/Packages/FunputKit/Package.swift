// swift-tools-version: 6.1

import PackageDescription

/// The Rust engine, which only exists for iOS.
let funputCore: Target.Dependency = .target(name: "FunputCore", condition: .when(platforms: [.iOS]))

/// Every library target is also a product under its own name.
let libraries = [
    "FunputEngine", "PersonalSuggestions", "PersonalDictionary", "KeyboardInput", "KeyboardLayout",
    "FunputShared", "ThemeSchema", "ThemeRuntime", "KeyboardRenderer", "KeyboardConfiguration",
    "KeyboardTouchCore", "KeyboardTouchUIKit",
]

let package = Package(
    name: "FunputKit",
    platforms: [.iOS(.v16)],
    products: libraries.map { .library(name: $0, targets: [$0]) },
    targets: [
        .binaryTarget(
            name: "FunputCore",
            path: "../../Frameworks/FunputCore.xcframework"
        ),
        .target(
            name: "FunputEngine",
            dependencies: [
                funputCore,
                // `KeyboardTouchEvidence` crosses from renderer to engine; `KeyboardLayout`
                // is the leaf module both already share, so this adds nothing to the graph.
                .target(name: "KeyboardLayout"),
            ]
        ),
        // The dictionary alone, light enough for the app to link: the engine and
        // nothing of the keyboard.
        .target(name: "PersonalDictionary", dependencies: [funputCore]),
        .target(
            name: "PersonalSuggestions",
            dependencies: [
                funputCore,
                .target(name: "PersonalDictionary"),
                .target(name: "KeyboardInput"),
                .target(name: "KeyboardLayout"),
                .target(name: "KeyboardRenderer"),
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
                .target(name: "FunputShared"),
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
            dependencies: ["KeyboardLayout", "KeyboardRenderer", "ThemeSchema", "FunputShared"]
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
        .testTarget(name: "FunputEngineTests", dependencies: ["FunputEngine", funputCore]),
        .testTarget(
            name: "PersonalSuggestionsTests",
            dependencies: ["PersonalSuggestions", "PersonalDictionary"]
        ),
        .testTarget(name: "PersonalDictionaryTests", dependencies: ["PersonalDictionary", funputCore]),
        .testTarget(
            name: "KeyboardInputTests",
            dependencies: ["KeyboardInput", "KeyboardLayout", "FunputShared", "FunputEngine"]
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
