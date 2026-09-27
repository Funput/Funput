// buildSrc is its own build and does not see the project's version catalog by default. Importing
// it keeps buildSrc's test dependencies on the same pinned versions as every module.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
