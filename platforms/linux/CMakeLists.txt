# Development/test superproject for the Linux platform.
#
# NOT a packaging build: `fcitx5/` and `ibus/` each stay their own top-level CMake
# project (that is what makes their CPack settings and install dirs independent),
# and platforms/linux/build.sh configures those directly. This exists so the shared
# code in `common/` can be built and tested on its own — with neither Fcitx5 nor
# IBus installed, since `common/compose/` links no framework symbol.
#
#   cmake -S platforms/linux -B platforms/linux/build/tests -DFUNPUT_BUILD_TESTS=ON
#   cmake --build platforms/linux/build/tests --parallel
#   ctest --test-dir platforms/linux/build/tests --output-on-failure

cmake_minimum_required(VERSION 3.16)
project(funput-linux-common VERSION 1.2026.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Debug)
endif()

# `enable_testing()` has to run in the top-level project for `ctest` to find the
# tests from the build root, so it lives here rather than in common/.
enable_testing()

add_subdirectory(common)
