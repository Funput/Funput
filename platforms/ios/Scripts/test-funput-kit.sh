#!/bin/sh
# Run FunputKit, including the FunputCore integration tests, on iOS Simulator.
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
IOS_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
PACKAGE_ROOT="$IOS_ROOT/Packages/FunputKit"
FRAMEWORK="$IOS_ROOT/Frameworks/FunputCore.xcframework"

if [ ! -d "$FRAMEWORK" ]; then
    echo "test-funput-kit: missing $FRAMEWORK" >&2
    echo "test-funput-kit: run $SCRIPT_DIR/bootstrap-ios.sh first" >&2
    exit 1
fi

if [ -n "${FUNPUT_IOS_TEST_DESTINATION:-}" ]; then
    DESTINATION="$FUNPUT_IOS_TEST_DESTINATION"
else
    SIMULATOR_ID="$(
        xcrun simctl list devices available | awk '
            /^-- iOS / { in_ios = 1; next }
            /^-- / { in_ios = 0 }
            in_ios {
                count = split($0, fields, "[()]")
                for (field_index = 2; field_index <= count; field_index += 2) {
                    if (length(fields[field_index]) == 36) {
                        print fields[field_index]
                        exit
                    }
                }
            }
        '
    )"
    if [ -z "$SIMULATOR_ID" ]; then
        echo "test-funput-kit: no available iOS Simulator found" >&2
        exit 1
    fi
    case "$(uname -m)" in
        arm64) SIMULATOR_ARCH="arm64" ;;
        x86_64) SIMULATOR_ARCH="x86_64" ;;
        *) SIMULATOR_ARCH="arm64" ;;
    esac
    DESTINATION="platform=iOS Simulator,arch=$SIMULATOR_ARCH,id=$SIMULATOR_ID"
fi

cd "$PACKAGE_ROOT"
exec xcodebuild test \
    -scheme FunputKit-Package \
    -destination "$DESTINATION"
