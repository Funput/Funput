cmake_minimum_required(VERSION 3.16)
# No languages: cargo builds the binary (see ../build.sh), this project only packages
# it. Declaring CXX here would make CMake hunt for a compiler it never uses.
project(funput-settings VERSION 1.2026.1 LANGUAGES NONE)

# The Settings GUI, its launcher and its icons, as a package of their own.
#
# They used to be installed by *both* shells, which on Debian/Ubuntu meant the second
# package could not be unpacked at all: dpkg refuses to overwrite a file owned by
# another package, identical contents or not. (RPM is lenient about byte-identical
# files, so the failure only showed on the platform most people use.) Splitting them
# out is what lets Fcitx5 and IBus be installed side by side.
#
# It is `funput-settings`, not `funput-common`: the binary inside is compiled Rust and
# therefore architecture-dependent, while a Debian `-common` package is conventionally
# `Architecture: all`.

include(GNUInstallDirs)

set(FUNPUT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../.." CACHE PATH "Repo app/ root")
set(FUNPUT_SETTINGS_BIN
    "${CMAKE_CURRENT_SOURCE_DIR}/target/release/funput-settings"
    CACHE FILEPATH "Path to the built GTK4 Settings binary")

# A hard error, unlike the warning the shells used to carry: an addon without the GUI
# still types, so packaging it alone was reasonable — a Settings package without the
# Settings app is nothing at all.
if(NOT EXISTS "${FUNPUT_SETTINGS_BIN}")
    message(FATAL_ERROR
            "funput-settings binary not found at ${FUNPUT_SETTINGS_BIN}. "
            "Build it first: cargo build --release --manifest-path "
            "platforms/linux/settings-gtk/Cargo.toml (platforms/linux/build.sh does this).")
endif()

install(PROGRAMS "${FUNPUT_SETTINGS_BIN}" DESTINATION "${CMAKE_INSTALL_BINDIR}")
install(FILES "${CMAKE_CURRENT_SOURCE_DIR}/../packaging/funput-settings.desktop"
        DESTINATION "${CMAKE_INSTALL_DATADIR}/applications")

# The same binary under a second launcher (`funput-settings --convert`). Linux has no
# tray, so the tray item Windows offers has nowhere to live; this is what stands in for
# it, and without it the converter would only be reachable through Settings.
#
# No packaging changes go with it: it lands in this package, and unlike the four files
# below it was never owned by a shell, so it needs no Replaces/Breaks.
install(FILES "${CMAKE_CURRENT_SOURCE_DIR}/../packaging/funput-convert.desktop"
        DESTINATION "${CMAKE_INSTALL_DATADIR}/applications")

# Shipped here rather than with a shell because the shells reference them by name
# (`Icon=funput`) and would otherwise each need their own copy — the very duplication
# this package exists to remove.
if(EXISTS "${FUNPUT_ROOT}/assets/svg/funput_logo.svg")
    install(FILES "${FUNPUT_ROOT}/assets/svg/funput_logo.svg"
            RENAME funput.svg
            DESTINATION "${CMAKE_INSTALL_DATADIR}/icons/hicolor/scalable/apps")
endif()
# White glyph for Fcitx5's tray when Vietnamese is off, traced from
# assets/logo/monochrome-light.png.
if(EXISTS "${FUNPUT_ROOT}/assets/svg/funput_logo_mono.svg")
    install(FILES "${FUNPUT_ROOT}/assets/svg/funput_logo_mono.svg"
            RENAME funput-mono.svg
            DESTINATION "${CMAKE_INSTALL_DATADIR}/icons/hicolor/scalable/apps")
endif()
if(EXISTS "${FUNPUT_ROOT}/assets/logo.png")
    install(FILES "${FUNPUT_ROOT}/assets/logo.png"
            RENAME funput.png
            DESTINATION "${CMAKE_INSTALL_DATADIR}/icons/hicolor/512x512/apps")
endif()

# --- Packaging (CPack: .deb + .rpm) ----------------------------------------
# Same shape as the two shells; see fcitx5/CMakeLists.txt for the reasoning behind
# each field, which is identical here.
set(FUNPUT_VERSION "${PROJECT_VERSION}" CACHE STRING "Package version")
set(CPACK_PACKAGING_INSTALL_PREFIX "/usr")
set(CPACK_PACKAGE_NAME "funput-settings")
set(CPACK_PACKAGE_VERSION "${FUNPUT_VERSION}")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Vietnamese input method (Telex, Full Telex & VNI) — Settings app")
set(CPACK_PACKAGE_CONTACT "Funput <hello@funput.app>")
set(CPACK_DEBIAN_PACKAGE_MAINTAINER "Funput <hello@funput.app>")
set(CPACK_DEBIAN_PACKAGE_SECTION "utils")
set(CPACK_DEBIAN_PACKAGE_HOMEPAGE "https://github.com/Funput/Funput")
# GTK4/libadwaita and the rest are picked up from the binary itself.
set(CPACK_DEBIAN_PACKAGE_SHLIBDEPS ON)

# These four files used to belong to the shells, so upgrading from any release before
# this one means taking them over from a package that still owns them — and dpkg
# refuses that unless it is declared (Debian Policy 7.6). `Breaks` alongside `Replaces`
# is what makes dpkg unpack in an order where the takeover is legal, rather than
# failing halfway through the transaction and leaving the shells unconfigured.
#
# Versioned on this release: a shell built from the same source no longer ships them,
# so only older ones are involved.
set(CPACK_DEBIAN_PACKAGE_REPLACES
    "funput (<< ${FUNPUT_VERSION}), funput-ibus (<< ${FUNPUT_VERSION})")
set(CPACK_DEBIAN_PACKAGE_BREAKS
    "funput (<< ${FUNPUT_VERSION}), funput-ibus (<< ${FUNPUT_VERSION})")

find_program(DPKG_CMD dpkg)
if(DPKG_CMD)
    execute_process(COMMAND ${DPKG_CMD} --print-architecture
        OUTPUT_VARIABLE FUNPUT_DEB_ARCH OUTPUT_STRIP_TRAILING_WHITESPACE)
else()
    set(FUNPUT_DEB_ARCH "amd64")
endif()
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "${FUNPUT_DEB_ARCH}")
set(CPACK_PACKAGE_FILE_NAME "funput-settings_${FUNPUT_VERSION}_${FUNPUT_DEB_ARCH}")

set(CPACK_GENERATOR "DEB;RPM")
set(CPACK_RPM_PACKAGE_LICENSE "MIT")
set(CPACK_RPM_PACKAGE_GROUP "Applications/System")
set(CPACK_RPM_PACKAGE_URL "https://github.com/Funput/Funput")
set(CPACK_RPM_PACKAGE_AUTOREQ ON)
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${CMAKE_SYSTEM_PROCESSOR}")
set(CPACK_RPM_FILE_NAME "funput-settings-${FUNPUT_VERSION}.${CMAKE_SYSTEM_PROCESSOR}.rpm")

include(CPack)
