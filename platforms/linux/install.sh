#!/usr/bin/env bash
# Funput Linux installer: detect the distro + arch, pick the matching package from
# the latest GitHub release, and install it with the native package manager.
#
# This is the "one command, auto-detect" front end over GitHub Releases — there is
# no hosted apt/dnf repo yet, so it downloads a versioned asset and installs it
# (no automatic upgrades; re-run this script to update). Apt and dnf are different
# repo formats, so a single repo can never serve every distro — only this kind of
# detect-then-fetch script gives the unified experience.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash
#   ./install.sh [--ibus | --fcitx5] [--version vX.Y.Z]
#
# No-sudo (user-local): install the engine into ~/.local — no root, no package
# manager. Needs an already-installed IBus or Fcitx5 daemon. IBus works right
# away; Fcitx5 also writes ~/.config/environment.d and needs a re-login.
#   curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash -s -- --user
#   ./install.sh --user [--ibus | --fcitx5] [--version vX.Y.Z]
#
# Framework: defaults to IBus, except KDE Plasma sessions default to Fcitx5.
# Override with --ibus / --fcitx5.
set -euo pipefail

REPO="Funput/Funput"
FRAMEWORK=""        # "ibus" | "fcitx5"; empty = auto-detect
VERSION="latest"    # "latest" or a tag like v1.2026.1
USER_INSTALL=0      # 1 = no-sudo, user-local install into ~/.local (IBus only)

die() { echo "Error: $*" >&2; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }

# Resolve a release asset's download URL by filename pattern (an extended regex,
# anchored to the asset basename). Prints the URL, or nothing if unmatched. Avoids
# a jq dependency by grepping browser_download_url out of the release API JSON.
resolve_url() {
  local pattern="$1" api
  if [ "$VERSION" = "latest" ]; then
    api="https://api.github.com/repos/${REPO}/releases/latest"
  else
    api="https://api.github.com/repos/${REPO}/releases/tags/${VERSION}"
  fi
  curl -fsSL "$api" \
    | grep -Eo '"browser_download_url": *"[^"]*"' \
    | sed -E 's/.*"(https[^"]*)".*/\1/' \
    | grep -E "/${pattern}$" \
    | head -n1 || true
}

# Download $1 to $2. `curl -f` fails on HTTP errors and HTTPS covers transport
# integrity; GitHub exposes a per-asset SHA-256 digest for manual verification.
download() {
  local url="$1" file="$2"
  echo "Downloading $(basename "$url")…"
  curl -fsSL "$url" -o "$file"
}

# Pick the framework from the running desktop when the user didn't force one:
# KDE Plasma → Fcitx5, everything else → IBus.
detect_framework() {
  [ -n "$FRAMEWORK" ] && return 0
  case " ${XDG_CURRENT_DESKTOP:-} " in
    *KDE*|*plasma*|*Plasma*) FRAMEWORK="fcitx5" ;;
    *)                       FRAMEWORK="ibus" ;;
  esac
  echo "Auto-selected framework: $FRAMEWORK (override with --ibus / --fcitx5)"
}

# The arch label used in the tarball / .deb names (amd64 / arm64).
user_pkg_arch() {
  case "$(uname -m)" in
    x86_64)        echo amd64 ;;
    aarch64|arm64) echo arm64 ;;
    *) die "unsupported CPU arch: $(uname -m)" ;;
  esac
}

# Shared across the --user flows: drop the Settings binary, icons and desktop entry
# into ~/.local, then refresh the icon/desktop caches (best-effort, never fatal).
place_common_assets() {
  local src="$1" data_home="$2" bin_dir="$3"
  install -m 0755 "$src/bin/funput-settings" "$bin_dir/"
  cp -rf "$src/share/icons"        "$data_home/"
  cp -rf "$src/share/applications" "$data_home/"
  gtk-update-icon-cache "$data_home/icons/hicolor" >/dev/null 2>&1 || true
  update-desktop-database "$data_home/applications" >/dev/null 2>&1 || true
}

# Warn if the user bin dir is not on PATH (so funput-settings + its .desktop resolve).
path_note() {
  case ":$PATH:" in
    *":$1:"*) : ;;
    *) echo "Note: $1 is not on PATH — add it so \"funput-settings\" launches from the app menu." ;;
  esac
}

# No-sudo, user-local IBus install. The engine binary is relocatable (its RPATH is
# $ORIGIN, so it finds libfunput_ffi.so beside it), and ibus-daemon scans
# $XDG_DATA_HOME/ibus/component — so laying the portable tarball into ~/.local and
# generating a component manifest that points <exec> at the real $HOME path is all
# it takes. No root, no package manager, no session env var.
user_install_ibus() {
  have curl || die "curl is required"
  have tar  || die "tar is required"
  have ibus || echo "Warning: 'ibus' is not on PATH — install the IBus daemon (needs your package manager / sudo) before the engine can run." >&2

  local pkg_arch; pkg_arch="$(user_pkg_arch)"
  local pattern="funput-ibus-[^/]*-${pkg_arch}-linux\.tar\.gz"
  echo "Looking up IBus tarball (${pkg_arch}) in ${REPO} ${VERSION} release…"
  local url; url="$(resolve_url "$pattern")"
  [ -n "$url" ] || die "no matching asset (${pattern}) in the ${VERSION} release"

  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  local file="$tmp/$(basename "$url")"
  download "$url" "$file"
  echo "Extracting…"; tar -xzf "$file" -C "$tmp"
  local src; src="$(find "$tmp" -maxdepth 1 -type d -name 'funput-ibus-*-linux' | head -n1)"
  [ -n "$src" ] || die "unexpected tarball layout (no funput-ibus-*-linux/ directory)"

  # lib/ and bin/ can live anywhere (the component <exec> is an absolute path); the
  # component XML MUST land under the ibus-scanned $XDG_DATA_HOME/ibus/component.
  local data_home="${XDG_DATA_HOME:-$HOME/.local/share}"
  local lib_dir="$HOME/.local/lib/funput"
  local bin_dir="$HOME/.local/bin"

  echo "Installing into $HOME/.local …"
  mkdir -p "$lib_dir" "$bin_dir" "$data_home/ibus/component"
  install -m 0755 "$src/lib/funput/ibus-engine-funput" "$lib_dir/"
  install -m 0644 "$src/lib/funput/libfunput_ffi.so"   "$lib_dir/"
  # ibus-daemon launches <exec> by absolute path, so point it at our real location.
  sed "s|@IBUS_ENGINE_PATH@|$lib_dir/ibus-engine-funput|g" \
    "$src/share/ibus/component/funput.xml.in" \
    > "$data_home/ibus/component/funput.xml"
  place_common_assets "$src" "$data_home" "$bin_dir"
  ibus restart >/dev/null 2>&1 || true

  echo
  echo "Installed (no sudo) into $HOME/.local."
  path_note "$bin_dir"
  echo "Next steps:"
  echo "  1. ibus restart            # if the engine is not listed yet"
  echo "  2. Settings → Keyboard → Input Sources → + → Vietnamese → Funput"
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  echo
  # No auto-updater on Linux yet — re-running fetches the latest release and
  # overwrites in place (then ibus restart loads the new engine).
  echo "To update later: re-run this installer (grabs the latest release)."
}

# Best-effort locate the system Fcitx5 addon dir (its layout varies by distro:
# Debian multiarch, Fedora /usr/lib64, Arch /usr/lib). FCITX_ADDON_DIRS overrides
# the built-in default, so we must re-list it or every other engine disappears.
fcitx5_system_addon_dir() {
  local pkg_arch="$1" d triplet
  case "$pkg_arch" in
    amd64) triplet="x86_64-linux-gnu" ;;
    arm64) triplet="aarch64-linux-gnu" ;;
  esac
  for d in "/usr/lib/${triplet}/fcitx5" /usr/lib64/fcitx5 /usr/lib/fcitx5; do
    [ -d "$d" ] && { echo "$d"; return 0; }
  done
  # Nothing found (Fcitx5 maybe not installed yet). Guess the Debian path for this
  # arch and warn — the user can fix the env file if it's wrong.
  echo "Warning: could not find the system Fcitx5 addon dir; guessing /usr/lib/${triplet}/fcitx5." >&2
  echo "/usr/lib/${triplet}/fcitx5"
}

# No-sudo, user-local Fcitx5 install. Config (.conf) is XDG-scanned like IBus, but
# the addon .so is NOT — Fcitx5 searches only its addon dirs, so we point it at
# ~/.local/lib/fcitx5 via FCITX_ADDON_DIRS in ~/.config/environment.d. That var
# OVERRIDES the default, so we re-list the system addon dir too, and it only takes
# effect after a re-login.
user_install_fcitx5() {
  have curl   || die "curl is required"
  have tar    || die "tar is required"
  have fcitx5 || echo "Warning: 'fcitx5' is not on PATH — install the Fcitx5 daemon (needs your package manager / sudo) before the engine can run." >&2

  local pkg_arch; pkg_arch="$(user_pkg_arch)"
  local pattern="funput-fcitx5-[^/]*-${pkg_arch}-linux\.tar\.gz"
  echo "Looking up Fcitx5 tarball (${pkg_arch}) in ${REPO} ${VERSION} release…"
  local url; url="$(resolve_url "$pattern")"
  [ -n "$url" ] || die "no matching asset (${pattern}) in the ${VERSION} release"

  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  local file="$tmp/$(basename "$url")"
  download "$url" "$file"
  echo "Extracting…"; tar -xzf "$file" -C "$tmp"
  local src; src="$(find "$tmp" -maxdepth 1 -type d -name 'funput-fcitx5-*-linux' | head -n1)"
  [ -n "$src" ] || die "unexpected tarball layout (no funput-fcitx5-*-linux/ directory)"

  local data_home="${XDG_DATA_HOME:-$HOME/.local/share}"
  local lib_dir="$HOME/.local/lib/fcitx5"
  local bin_dir="$HOME/.local/bin"

  echo "Installing into $HOME/.local …"
  mkdir -p "$lib_dir" "$bin_dir" "$data_home/fcitx5/addon" "$data_home/fcitx5/inputmethod"
  # Addon shared library + Rust core side by side (libfunput.so finds
  # libfunput_ffi.so via $ORIGIN). Config is loaded by name, so no path rewrite.
  install -m 0644 "$src/lib/fcitx5/libfunput.so"     "$lib_dir/"
  install -m 0644 "$src/lib/fcitx5/libfunput_ffi.so" "$lib_dir/"
  cp -f "$src/share/fcitx5/addon/funput.conf"       "$data_home/fcitx5/addon/"
  cp -f "$src/share/fcitx5/inputmethod/funput.conf" "$data_home/fcitx5/inputmethod/"
  place_common_assets "$src" "$data_home" "$bin_dir"

  # Tell Fcitx5 where our addon .so lives. Keep the system dir first so the built-in
  # engines (keyboard, spell, …) still load.
  local sys_addon; sys_addon="$(fcitx5_system_addon_dir "$pkg_arch")"
  local envd="${XDG_CONFIG_HOME:-$HOME/.config}/environment.d"
  mkdir -p "$envd"
  printf 'FCITX_ADDON_DIRS=%s:%s\n' "$sys_addon" "$lib_dir" > "$envd/funput.conf"

  echo
  echo "Installed (no sudo) into $HOME/.local."
  path_note "$bin_dir"
  echo "IMPORTANT — Fcitx5 loads the addon .so only after the session env updates:"
  echo "  Wrote $envd/funput.conf → FCITX_ADDON_DIRS=$sys_addon:$lib_dir"
  echo "  1. Log out and back in (applies environment.d), OR for a quick test now:"
  echo "       FCITX_ADDON_DIRS=$sys_addon:$lib_dir fcitx5 -r -d"
  echo "  2. fcitx5-configtool → + → add Funput (Vietnamese group)"
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  echo
  echo "To update later: re-run this installer (grabs the latest release)."
}

while [ $# -gt 0 ]; do
  case "$1" in
    --ibus)    FRAMEWORK="ibus" ;;
    --fcitx5)  FRAMEWORK="fcitx5" ;;
    --user)    USER_INSTALL=1 ;;
    --version) shift; VERSION="${1:?--version needs a tag}" ;;
    -h|--help)
      # Print the usage header (everything between line 2 and `set -euo …`).
      sed -n '2,/^set -euo/p' "$0" | sed -e '/^set -euo/d' -e 's/^# \{0,1\}//'
      exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
  shift
done

# No-sudo path: user-local install into ~/.local, then done — it needs none of the
# distro-family / package-manager logic below.
if [ "$USER_INSTALL" -eq 1 ]; then
  detect_framework
  case "$FRAMEWORK" in
    ibus)   user_install_ibus ;;
    fcitx5) user_install_fcitx5 ;;
    *)      die "unknown framework: $FRAMEWORK" ;;
  esac
  exit 0
fi

# --- Distro family ---------------------------------------------------------
[ -r /etc/os-release ] || die "cannot read /etc/os-release; unsupported system"
# shellcheck disable=SC1091
. /etc/os-release
# Match on ID first, then ID_LIKE so derivatives (Linux Mint, Pop!_OS, Nobara,
# openSUSE variants, Manjaro/EndeavourOS) resolve to the right family.
case " ${ID:-} ${ID_LIKE:-} " in
  *" debian "*|*" ubuntu "*) FAMILY="debian" ;;
  *" fedora "*|*" rhel "*)   FAMILY="fedora" ;;
  *" suse "*|*" opensuse "*) FAMILY="suse" ;;
  *" arch "*)                FAMILY="arch" ;;
  *) die "unsupported distro (ID=${ID:-?}, ID_LIKE=${ID_LIKE:-?}). Build from source: platforms/linux/build.sh" ;;
esac

# Arch is community-packaged via the AUR, not a release asset.
if [ "$FAMILY" = "arch" ]; then
  cat >&2 <<'EOF'
Arch Linux is packaged through the AUR, not the GitHub release assets.
Install with an AUR helper, e.g.:
  yay -S funput-ibus     # IBus (GNOME)
  yay -S funput          # Fcitx5 (KDE / full features)
EOF
  exit 1
fi

# --- Framework -------------------------------------------------------------
detect_framework

# --- Arch + package-name pattern -------------------------------------------
# .deb uses amd64/arm64; .rpm uses x86_64/aarch64. The CPack file names are:
#   deb  funput_<v>_<arch>.deb        funput-ibus_<v>_<arch>.deb
#   rpm  funput-<v>.<arch>.rpm        funput-ibus-<v>.<arch>.rpm
MACHINE="$(uname -m)"
if [ "$FAMILY" = "debian" ]; then
  FORMAT="deb"
  case "$MACHINE" in
    x86_64) PKG_ARCH="amd64" ;;
    aarch64|arm64) PKG_ARCH="arm64" ;;
    *) die "unsupported CPU arch: $MACHINE" ;;
  esac
  if [ "$FRAMEWORK" = "ibus" ]; then
    PATTERN="funput-ibus_[^/]*_${PKG_ARCH}\.deb"
  else
    PATTERN="funput_[^/]*_${PKG_ARCH}\.deb"
  fi
else
  # fedora + suse → .rpm
  FORMAT="rpm"
  case "$MACHINE" in
    x86_64) PKG_ARCH="x86_64" ;;
    aarch64|arm64) PKG_ARCH="aarch64" ;;
    *) die "unsupported CPU arch: $MACHINE" ;;
  esac
  if [ "$FRAMEWORK" = "ibus" ]; then
    PATTERN="funput-ibus-[^/]*\.${PKG_ARCH}\.rpm"
  else
    # funput- followed by a digit = version, to not match funput-ibus-*.
    PATTERN="funput-[0-9][^/]*\.${PKG_ARCH}\.rpm"
  fi
fi

# --- Resolve + download the release asset ----------------------------------
have curl || die "curl is required"
echo "Looking up ${FRAMEWORK} ${FORMAT} (${PKG_ARCH}) in ${REPO} ${VERSION} release…"
URL="$(resolve_url "$PATTERN")"
[ -n "$URL" ] || die "no matching asset (${PATTERN}) in the ${VERSION} release"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FILE="$TMP/$(basename "$URL")"
download "$URL" "$FILE"

# --- Install ---------------------------------------------------------------
SUDO=""
[ "$(id -u)" -eq 0 ] || SUDO="sudo"
echo "Installing $(basename "$FILE")…"
case "$FAMILY" in
  debian) $SUDO apt-get install -y "$FILE" ;;
  fedora) $SUDO dnf install -y "$FILE" ;;
  suse)   $SUDO zypper --non-interactive install --allow-unsigned-rpm "$FILE" ;;
esac

# --- Post-install hint -----------------------------------------------------
echo
echo "Installed. Next steps:"
if [ "$FRAMEWORK" = "ibus" ]; then
  echo "  1. ibus restart            # load the newly registered engine"
  echo "  2. Settings → Keyboard → Input Sources → + → Vietnamese → Funput"
else
  echo "  1. fcitx5-configtool       # + → add Funput (Vietnamese group)"
  echo "  2. log out/in if Fcitx5 was not already running"
fi
echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
