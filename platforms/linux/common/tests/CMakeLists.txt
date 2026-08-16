# Unit tests for the shared Linux code. Only configured when FUNPUT_BUILD_TESTS is
# ON (see ../CMakeLists.txt), so neither the .deb/.rpm builds nor a distro packager
# ever needs the network fetch below.
#
# These link `funput_linux_common`, which carries libfunput_ffi.so — so the
# composer tests drive the *real* Rust engine across the C ABI rather than a mock.

include(FetchContent)
# Build doctest as a dependency only: no tests of its own, no install rules.
set(DOCTEST_WITH_TESTS OFF CACHE BOOL "" FORCE)
set(DOCTEST_NO_INSTALL ON CACHE BOOL "" FORCE)
FetchContent_Declare(doctest
    GIT_REPOSITORY https://github.com/doctest/doctest.git
    GIT_TAG v2.4.12
    GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(doctest)

# One test file per source file, in a tree that mirrors common/.
add_executable(funput_linux_tests
    main.cpp
    compose/key/boundary_tests.cpp
    compose/key/classify_tests.cpp
    compose/composer/keys_tests.cpp
    compose/composer/state_tests.cpp
    compose/composer/nonpreedit_tests.cpp
    compose/composer/retone_tests.cpp
    compose/composer/verify_tests.cpp
    ffi/utf8_tests.cpp
    settings/settings_tests.cpp)

target_link_libraries(funput_linux_tests PRIVATE funput_linux_common doctest::doctest)
# So the nested test files can reach support.h by its bare name.
target_include_directories(funput_linux_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")

# libfunput_ffi.so is not installed anywhere at test time; resolve it from the
# build tree the same way the shipped shells resolve it from their install dir.
get_filename_component(_ffi_dir "${FUNPUT_FFI_SO}" DIRECTORY)
set_target_properties(funput_linux_tests PROPERTIES
    BUILD_RPATH "${_ffi_dir}")

# doctest ships a CMake helper that registers each TEST_CASE with CTest
# individually, so a failure names the case rather than the whole binary.
include("${doctest_SOURCE_DIR}/scripts/cmake/doctest.cmake")
doctest_discover_tests(funput_linux_tests)
