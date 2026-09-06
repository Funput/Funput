#!/usr/bin/env bash
# Funput Linux installer: detect the distro + arch and install with the native
# package manager.
#
# By default it adds the signed Funput repository (repo.funput.app) and installs
# from it, so `apt/dnf/zypper upgrade` picks up later releases like any other
# system package. Apt and rpm are different repo formats, so a single repo can
# never serve every distro — only this kind of detect-then-configure script gives
# the unified experience.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash
#   ./install.sh [--ibus | --fcitx5] [--no-repo] [--version vX.Y.Z]
#
# --no-repo: skip the repository and install a versioned asset straight from
# GitHub Releases instead. No automatic upgrades — re-run to update. Implied by
# --version, because the repository only ever carries the newest release. Not
# available on Arch, which has no release asset — use the repository or --user.
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
REPO_URL="https://repo.funput.app"   # the signed apt/dnf/zypper repository
FRAMEWORK=""        # "ibus" | "fcitx5"; empty = auto-detect
VERSION="latest"    # "latest" or a tag like v1.2026.1
USER_INSTALL=0      # 1 = no-sudo, user-local install into ~/.local (IBus only)
USE_REPO=1          # 0 = --no-repo: fetch a release asset instead

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
  fcitx5_wayland_hint
  echo
  echo "To update later: re-run this installer (grabs the latest release)."
}

# Funput is an Fcitx5 engine; apps only reach it if the *session* talks to Fcitx5.
# That wiring is desktop-specific (GNOME vs KDE) and Funput must not write a
# global GTK_IM_MODULE=fcitx block — KWin blinks the candidate window if you do.
# Official: https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland
fcitx5_wayland_hint() {
  echo "  Wayland session env (by desktop, then log out):"
  echo "    GNOME  ~/.config/environment.d/fcitx5.conf :"
  echo "           XMODIFIERS=@im=fcitx"
  echo "           QT_IM_MODULE=fcitx"
  echo "           (leave GTK_IM_MODULE unset — GNOME uses Fcitx5's ibus frontend)"
  echo "    KDE    XMODIFIERS=@im=fcitx only; do not set GTK/QT/SDL_IM_MODULE"
  echo "  https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland"
}

# What to do once the package is on disk. Shared by the repository path and the
# --no-repo one, which install the same package by different routes.
post_install_hint() {
  echo
  echo "Installed. Next steps:"
  if [ "$FRAMEWORK" = "ibus" ]; then
    echo "  1. ibus restart            # load the newly registered engine"
    echo "  2. Settings → Keyboard → Input Sources → + → Vietnamese → Funput"
  else
    echo "  1. fcitx5-configtool       # + → add Funput (Vietnamese group)"
    echo "  2. log out/in if Fcitx5 was not already running"
    fcitx5_wayland_hint
  fi
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  if [ "$USE_REPO" -eq 0 ]; then
    echo
    echo "Installed from a release asset, so this will not upgrade itself."
    echo "For automatic updates, re-run without --version/--no-repo to add ${REPO_URL}."
  fi
}

# --- The signed repository (the default path) ------------------------------
# Adding the repo instead of dropping a downloaded package in is what buys the
# user automatic upgrades; every other path here has to be re-run by hand. The
# packages it serves are GPG-signed, so none of these needs an
# unsigned-package escape hatch.

# Debian/Ubuntu. The deb822 `.sources` format, not a one-line `.list`: apt 3.0
# (Debian 13, Ubuntu 25.04) deprecates the latter and says so on every run. A flat
# repository is spelled `Suites: ./` with no Components.
repo_add_debian() {
  local keyring="/usr/share/keyrings/funput.asc"
  echo "Adding ${REPO_URL} (apt)…"
  $SUDO install -d /usr/share/keyrings
  curl -fsSL "${REPO_URL}/funput.asc" | $SUDO tee "$keyring" >/dev/null
  printf 'Types: deb\nURIs: %s/deb\nSuites: ./\nSigned-By: %s\n' \
    "$REPO_URL" "$keyring" | $SUDO tee /etc/apt/sources.list.d/funput.sources >/dev/null
  $SUDO apt-get update
}

# Fedora/RHEL. Written straight into /etc/yum.repos.d rather than through
# `dnf config-manager --add-repo`, which dnf5 (Fedora 41+) removed; a plain file
# drop works on both dnf4 and dnf5. The key is not imported here — funput.repo
# carries `gpgkey=` and dnf fetches it on first install.
repo_add_fedora() {
  echo "Adding ${REPO_URL} (dnf)…"
  curl -fsSL "${REPO_URL}/funput.repo" | $SUDO tee /etc/yum.repos.d/funput.repo >/dev/null
}

# openSUSE. zypper wants the key in the rpm database up front, unlike dnf.
repo_add_suse() {
  echo "Adding ${REPO_URL} (zypper)…"
  $SUDO rpm --import "${REPO_URL}/funput.asc"
  $SUDO zypper --non-interactive addrepo -gf "${REPO_URL}/rpm/" funput
  $SUDO zypper --non-interactive refresh
}

# Arch. Two steps no other package manager needs: pacman keeps its own keyring,
# so the key has to be imported and locally signed before any signature verifies,
# and the repo section goes into pacman.conf itself rather than a drop-in file.
# x86_64 only, which is all Arch officially supports.
repo_add_arch() {
  [ "$(uname -m)" = "x86_64" ] || die "the Funput pacman repo is x86_64 only (Arch Linux ARM is a separate distribution). Try --user."
  echo "Adding ${REPO_URL} (pacman)…"
  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  curl -fsSL "${REPO_URL}/funput.asc" -o "$tmp/funput.asc"
  $SUDO pacman-key --add "$tmp/funput.asc"
  # Local signing is what promotes the key from "known" to "trusted"; without it
  # pacman reports every package as signed by an unknown key.
  $SUDO pacman-key --lsign-key hello@funput.app

  # Appended, not written: pacman.conf is one file holding every repo, and the
  # order of sections is the priority order. Skip if it is already there so
  # re-running does not stack duplicate sections.
  if ! grep -q '^\[funput\]' /etc/pacman.conf; then
    printf '\n[funput]\nServer = %s/arch/$arch\n' "$REPO_URL" \
      | $SUDO tee -a /etc/pacman.conf >/dev/null
  fi
  $SUDO pacman -Sy
}

# Install (or upgrade to) the package for the chosen framework from the repo.
repo_install() {
  local pkg="funput"
  [ "$FRAMEWORK" = "ibus" ] && pkg="funput-ibus"
  echo "Installing ${pkg} from ${REPO_URL}…"
  case "$FAMILY" in
    debian) $SUDO apt-get install -y "$pkg" ;;
    fedora) $SUDO dnf install -y "$pkg" ;;
    suse)   $SUDO zypper --non-interactive install "$pkg" ;;
    arch)   $SUDO pacman -S --noconfirm "$pkg" ;;
  esac
}

while [ $# -gt 0 ]; do
  case "$1" in
    --ibus)    FRAMEWORK="ibus" ;;
    --fcitx5)  FRAMEWORK="fcitx5" ;;
    --user)    USER_INSTALL=1 ;;
    --no-repo) USE_REPO=0 ;;
    # Pinning a version means leaving the repository behind: it is rebuilt from
    # the current release each time and serves nothing else.
    --version) shift; VERSION="${1:?--version needs a tag}"; USE_REPO=0 ;;
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

SUDO=""
[ "$(id -u)" -eq 0 ] || SUDO="sudo"

# Arch has no release asset: it is a rolling source distro, so the .deb/.rpm
# builds have nothing to offer it. What repo.funput.app serves instead is a
# pacman repository of the same three packages, under the same key. Handled
# entirely by repo_add_arch below, so it falls through with the others.

# --- Framework -------------------------------------------------------------
detect_framework

# --- Repository path (the default) -----------------------------------------
# Everything below this block is the --no-repo fallback: one versioned asset,
# updated only by re-running the script.
if [ "$USE_REPO" -eq 1 ]; then
  have curl || die "curl is required"
  case "$FAMILY" in
    debian) repo_add_debian ;;
    fedora) repo_add_fedora ;;
    suse)   repo_add_suse ;;
    arch)   repo_add_arch ;;
  esac
  repo_install
  post_install_hint
  exit 0
fi

# Past this point every path resolves a release asset, and Arch has none — the
# rpm branch below would otherwise pick up a Fedora package for it.
if [ "$FAMILY" = "arch" ]; then
  die "Arch has no release asset to install (--no-repo / --version cannot work here). Use the repository (re-run without those flags), or --user for a no-sudo install into ~/.local."
fi

# --- CPU arch + package-name pattern ---------------------------------------
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
echo "Installing $(basename "$FILE")…"
case "$FAMILY" in
  debian) $SUDO apt-get install -y "$FILE" ;;
  fedora) $SUDO dnf install -y "$FILE" ;;
  suse)   $SUDO zypper --non-interactive install --allow-unsigned-rpm "$FILE" ;;
esac

post_install_hint
