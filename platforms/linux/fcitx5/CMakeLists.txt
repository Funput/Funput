cmake_minimum_required(VERSION 3.16)
project(funput-fcitx5 VERSION 1.2026.1 LANGUAGES CXX)

# C++20: Fcitx5 5.1+ public headers (log.h, stringutils.h) use std::span and
# std::string_view::starts_with/ends_with. Fedora ships that newer Fcitx5, so 17
# fails to compile its headers; Ubuntu's older Fcitx5 builds fine at 20 too.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Install into /usr, not the /usr/local default. Fcitx5 only scans the system
# addon dirs (/usr/lib/<triplet>/fcitx5, /usr/share/fcitx5) — it ignores
# /usr/local. The /usr prefix also makes GNUInstallDirs use the Debian multiarch
# libdir, so FCITX_INSTALL_ADDONDIR resolves correctly. Must run before
# find_package(Fcitx5Utils), which computes those install dirs.
if(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
    set(CMAKE_INSTALL_PREFIX "/usr" CACHE PATH "Install prefix" FORCE)
endif()

find_package(Fcitx5Core REQUIRED)
find_package(Fcitx5Utils REQUIRED)

include(GNUInstallDirs)

# --- Shared Linux code (settings reader + FFI wrapper + Rust cdylib build) --
# Lives in platforms/linux/common, shared with the IBus shell. Owns building
# libfunput_ffi.so and exposes (PUBLIC) the common headers, funput.h, and the .so
# to link — plus the FUNPUT_FFI_SO cache var used below.
add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../common"
                 "${CMAKE_BINARY_DIR}/common")

# --- Fcitx5 addon -> libfunput.so ------------------------------------------
add_library(funput MODULE
    src/funput_engine.cpp
    src/funput_input.cpp
    src/funput_client.cpp
)
add_dependencies(funput funput_ffi)

# funput_linux_common carries the FFI include dir + the .so transitively.
target_link_libraries(funput PRIVATE
    Fcitx5::Core
    Fcitx5::Utils
    funput_linux_common
)

# libfunput_ffi.so is installed next to libfunput.so; resolve it via $ORIGIN so
# no system-wide ldconfig entry is needed.
set_target_properties(funput PROPERTIES
    INSTALL_RPATH "$ORIGIN"
    BUILD_WITH_INSTALL_RPATH ON
)

# --- Install ---------------------------------------------------------------
# Addon .so + the Rust core, side by side in the fcitx5 addon dir.
install(TARGETS funput DESTINATION "${FCITX_INSTALL_ADDONDIR}")
install(FILES "${FUNPUT_FFI_SO}" DESTINATION "${FCITX_INSTALL_ADDONDIR}")

# Addon metadata + input-method registration.
configure_file(data/funput-addon.conf.in funput-addon.conf @ONLY)
configure_file(data/funput.conf.in funput.conf @ONLY)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/funput-addon.conf"
        RENAME funput.conf
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/addon")
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/funput.conf"
        DESTINATION "${FCITX_INSTALL_PKGDATADIR}/inputmethod")

# --- Packaging (CPack: .deb + .rpm) ----------------------------------------
# CI overrides this from the git tag (-DFUNPUT_VERSION=...); otherwise project().
set(FUNPUT_VERSION "${PROJECT_VERSION}" CACHE STRING "Package version")
# Lay the relative installs (bin, share/…) under /usr to match the absolute
# Fcitx5 addon dirs — everything in the package lives under /usr.
set(CPACK_PACKAGING_INSTALL_PREFIX "/usr")
set(CPACK_PACKAGE_NAME "funput")
set(CPACK_PACKAGE_VERSION "${FUNPUT_VERSION}")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Vietnamese input method (Telex, Full Telex & VNI) — Fcitx5 addon")
set(CPACK_PACKAGE_CONTACT "Funput <hello@funput.app>")
set(CPACK_DEBIAN_PACKAGE_MAINTAINER "Funput <hello@funput.app>")
set(CPACK_DEBIAN_PACKAGE_SECTION "utils")
set(CPACK_DEBIAN_PACKAGE_HOMEPAGE "https://github.com/Funput/Funput")
# fcitx5 must be present; the rest are picked up automatically.
# `funput-settings` is pinned to the exact same version, not merely required. The
# Settings app writes settings.json by serializing its whole struct, so a copy older
# than this addon deletes any key the addon has learned since — a data loss that bit
# twice during development. Documentation asked for them to ship together; this makes
# the package manager enforce it.
set(CPACK_DEBIAN_PACKAGE_DEPENDS
    "fcitx5, funput-settings (= ${FUNPUT_VERSION})")
set(CPACK_DEBIAN_PACKAGE_SHLIBDEPS ON)
# Architecture from the build host (amd64 / arm64 / …). CPack sets the control
# field from this; mirror it in the filename so an arm64 build isn't mislabelled
# "amd64". Apple Silicon VMs (Apple Virtualization) are arm64, so a host-arch deb
# matters there.
find_program(DPKG_CMD dpkg)
if(DPKG_CMD)
    execute_process(COMMAND ${DPKG_CMD} --print-architecture
        OUTPUT_VARIABLE FUNPUT_DEB_ARCH OUTPUT_STRIP_TRAILING_WHITESPACE)
else()
    set(FUNPUT_DEB_ARCH "amd64")
endif()
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "${FUNPUT_DEB_ARCH}")
set(CPACK_PACKAGE_FILE_NAME "funput_${FUNPUT_VERSION}_${FUNPUT_DEB_ARCH}")

# --- .rpm packaging (CPack, Fedora/openSUSE) -------------------------------
# Both generators are listed; CI selects one per host with `cpack -G DEB|RPM`
# (a Fedora runner produces the .rpm so GNUInstallDirs/FCITX_INSTALL_ADDONDIR
# resolve the rpm-distro libdir — /usr/lib64 — and shlib deps match Fedora pkgs).
# The CPACK_RPM_* fields below are ignored when the DEB generator runs.
set(CPACK_GENERATOR "DEB;RPM")
set(CPACK_RPM_PACKAGE_LICENSE "MIT")
set(CPACK_RPM_PACKAGE_GROUP "Applications/System")
set(CPACK_RPM_PACKAGE_URL "https://github.com/Funput/Funput")
# fcitx5 must be present; AUTOREQ picks up the shared-lib deps automatically.
set(CPACK_RPM_PACKAGE_REQUIRES "fcitx5, funput-settings = ${FUNPUT_VERSION}")
set(CPACK_RPM_PACKAGE_AUTOREQ ON)
# RPM arch names differ from dpkg's (x86_64/aarch64, not amd64/arm64); take them
# from the native build host. dpkg is absent on Fedora, so this is independent of
# FUNPUT_DEB_ARCH above (which falls back to amd64 there but is unused for RPM).
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${CMAKE_SYSTEM_PROCESSOR}")
set(CPACK_RPM_FILE_NAME "funput-${FUNPUT_VERSION}.${CMAKE_SYSTEM_PROCESSOR}.rpm")

include(CPack)
