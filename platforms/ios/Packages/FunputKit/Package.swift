// swift-tools-version: 6.1

import PackageDescription

let package = Package(
    name: "FunputKit",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "KeyboardLayout", targets: ["KeyboardLayout"]),
        .library(name: "ThemeSchema", targets: ["ThemeSchema"]),
        .library(name: "KeyboardRenderer", targets: ["KeyboardRenderer"]),
    ],
    targets: [
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
    ]
)
