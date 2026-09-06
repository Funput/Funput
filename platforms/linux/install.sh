#!/usr/bin/env bash
# Funput Linux installer: detect the distro + arch and install with the native
# package manager.
#
# Part of Funput — https://github.com/Funput/Funput — MIT licensed.
#
# By default it adds the signed Funput repository (repo.funput.app) and installs
# from it, so `apt/dnf/zypper upgrade` picks up later releases like any other
# system package. Apt and rpm are different repo formats, so a single repo can
# never serve every distro — only this kind of detect-then-configure script gives
# the unified experience.
#
# Framework: Fcitx5 on every desktop, --ibus for the IBus engine instead. Fcitx5
# is the fuller shell — it can draw a panel preedit for clients that hide the
# client one (WPS Office), which the IBus side has no channel for. Outside a KDE
# session it does cost one-time session wiring (environment.d + a re-login),
# which this script prints rather than writes: those variables belong to the
# session, not to Funput.
#
# This file is what a `curl … | bash` reader is being asked to trust, so:
#   - it prints the plan (distro, framework, package, channel) before it acts,
#     and `--dry-run` prints without acting at all;
#   - every asset it fetches from GitHub Releases is checked against the SHA-256
#     digest the release API publishes for that asset;
#   - nothing fails silently: an unexpected error names the line and the issue
#     tracker rather than leaving a half-install behind without a word.
#
# usage() below is the single source of `--help`.

# -E keeps the ERR trap alive inside functions; -u turns a typo into a failure
# instead of an empty string; pipefail stops `curl … | tee` reporting success on
# an HTTP error.
set -Eeuo pipefail

REPO="Funput/Funput"
API="https://api.github.com/repos/${REPO}"
REPO_URL="https://repo.funput.app"   # the signed apt/dnf/zypper/pacman repository
ISSUES_URL="https://github.com/${REPO}/issues"

FRAMEWORK=""        # "fcitx5" | "ibus"; empty = the default, fcitx5
VERSION="latest"    # "latest" or a tag like v1.2026.1
USER_INSTALL=0      # 1 = no-sudo, user-local install into ~/.local (either framework)
USE_REPO=1          # 0 = --no-repo: fetch a release asset instead
DRY_RUN=0           # 1 = print what would happen, change nothing

# --- Output ----------------------------------------------------------------
# Colour only when stdout is a terminal and the user has not opted out. Under
# `curl … | bash` the script arrives on stdin, so stdout is still the terminal
# and this stays colourful — which is the common case worth getting right.
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  C_OFF=$'\033[0m'; C_DIM=$'\033[2m'; C_BLUE=$'\033[34m'
  C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'
else
  C_OFF=""; C_DIM=""; C_BLUE=""; C_YELLOW=""; C_RED=""
fi

info() { printf '%s==>%s %s\n' "$C_BLUE" "$C_OFF" "$*"; }
note() { printf '    %s%s%s\n' "$C_DIM" "$*" "$C_OFF"; }
warn() { printf '%swarning:%s %s\n' "$C_YELLOW" "$C_OFF" "$*" >&2; }
die()  { printf '%serror:%s %s\n' "$C_RED" "$C_OFF" "$*" >&2; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }

# An unexpected failure (a command nobody guarded) should not read like a crash.
# `die` exits rather than returning non-zero, so intentional errors never reach
# here and this only ever fires on a genuine bug or a broken environment.
on_error() {
  local code=$? line="${1:-?}"
  printf '%serror:%s install.sh stopped unexpectedly at line %s (exit %s).\n' \
    "$C_RED" "$C_OFF" "$line" "$code" >&2
  printf '       Nothing further was installed. Please report the output above: %s\n' \
    "$ISSUES_URL" >&2
}
trap 'on_error $LINENO' ERR

# Run a command that changes the system. Under --dry-run it is printed, not run,
# which is the whole promise of that flag: every mutation goes through here or
# through write_file/write_root below.
run() {
  if [ "$DRY_RUN" -eq 1 ]; then printf '    %s$ %s%s\n' "$C_DIM" "$*" "$C_OFF"; return 0; fi
  "$@"
}

# The one shape `run` cannot express: writing a stream to a file. write_root goes
# through sudo tee for paths we do not own.
write_file() {
  if [ "$DRY_RUN" -eq 1 ]; then printf '    %s$ write %s%s\n' "$C_DIM" "$1" "$C_OFF"; cat >/dev/null; return 0; fi
  cat > "$1"
}
write_root() {
  if [ "$DRY_RUN" -eq 1 ]; then printf '    %s$ write %s (sudo)%s\n' "$C_DIM" "$1" "$C_OFF"; cat >/dev/null; return 0; fi
  $SUDO tee "$1" >/dev/null
}
# Fetch a URL straight into a root-owned path. Its own helper so --dry-run can
# print the whole pipeline and make no network call at all.
fetch_to_root() {
  if [ "$DRY_RUN" -eq 1 ]; then
    printf '    %s$ curl -fsSL %s | sudo tee %s%s\n' "$C_DIM" "$1" "$2" "$C_OFF"; return 0
  fi
  curl -fsSL "$1" | $SUDO tee "$2" >/dev/null
}
append_root() {
  if [ "$DRY_RUN" -eq 1 ]; then printf '    %s$ append to %s (sudo)%s\n' "$C_DIM" "$1" "$C_OFF"; cat >/dev/null; return 0; fi
  $SUDO tee -a "$1" >/dev/null
}

usage() {
  cat <<'EOF'
Funput — Vietnamese input method for Linux
Installs the Fcitx5 (default) or IBus engine with your native package manager.

USAGE
  curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash
  ./install.sh [OPTIONS]

OPTIONS
  --fcitx5        Install the Fcitx5 engine (package "funput"). The default.
  --ibus          Install the IBus engine (package "funput-ibus") instead.
  --user          Install into ~/.local — no root, no package manager. Needs an
                  already-installed Fcitx5 or IBus daemon. With Fcitx5 this also
                  writes ~/.config/environment.d and needs a re-login; IBus works
                  right away.
  --no-repo       Skip the repository; install one versioned package straight
                  from GitHub Releases. No automatic upgrades — re-run to update.
                  Not available on Arch, which has no release asset.
  --version TAG   Install a specific release, e.g. --version v1.2026.28. Implies
                  --no-repo, because the repository only carries the newest one.
  --dry-run       Print every command that would run, and run none of them.
  -h, --help      Show this help.

ENVIRONMENT
  NO_COLOR        Set to any value to disable coloured output.

By default the signed repository at https://repo.funput.app is added, so later
releases arrive through apt/dnf/zypper/pacman like any other system package.

Docs   https://docs.funput.app/docs/install/linux
Issues https://github.com/Funput/Funput/issues
EOF
}

# --- Release assets --------------------------------------------------------

# Resolve a release asset by name pattern (an extended regex, anchored to the
# whole asset name). Prints "<url><TAB><sha256>", or nothing when unmatched.
#
# No jq dependency: the release API pretty-prints one field per line, and within
# an asset object "name" precedes "digest", which precedes "browser_download_url"
# — so a single awk pass keeps the three together. Nothing is emitted until a
# browser_download_url closes an asset, so the release's own "name" and the
# uploader object cannot be mistaken for one.
resolve_asset() {
  local pattern="$1" api json
  if [ "$VERSION" = "latest" ]; then
    api="${API}/releases/latest"
  else
    api="${API}/releases/tags/${VERSION}"
  fi
  # Fetched into a variable rather than piped, so an unreachable API or a wrong
  # --version tag says so, instead of arriving at awk as an empty stream and
  # being reported as "no matching asset".
  json="$(curl -fsSL "$api")" \
    || die "cannot read the ${VERSION} release from GitHub.
       Tried ${api} — check your network, and the tag if you passed --version."
  printf '%s\n' "$json" | awk -v pat="^${pattern}$" '
    function val(line,   v) {
      v = line; sub(/^[^:]*:[[:space:]]*"?/, "", v); sub(/"?,?$/, "", v); return v
    }
    /^[[:space:]]*"name":[[:space:]]/                 { name = val($0); next }
    /^[[:space:]]*"digest":[[:space:]]/               { dg   = val($0); next }
    /^[[:space:]]*"browser_download_url":[[:space:]]/ {
      if (name ~ pat) { sub(/^sha256:/, "", dg); print val($0) "\t" dg; exit }
      name = ""; dg = ""
    }'
}

# Check a downloaded file against the digest the release API published for it.
# HTTPS already covers the transport; this covers the rest of the path — a
# truncated download, a mirror, a tampered artifact — and is the reason the docs
# no longer ask anyone to verify a checksum by hand.
verify_sha256() {
  local file="$1" want="$2" got
  case "$want" in
    ""|null|-) warn "the release metadata carries no checksum for $(basename "$file"); skipping verification"; return 0 ;;
  esac
  if   have sha256sum; then got="$(sha256sum "$file" | cut -d' ' -f1)"
  elif have shasum;    then got="$(shasum -a 256 "$file" | cut -d' ' -f1)"
  else warn "neither sha256sum nor shasum found; skipping checksum verification"; return 0
  fi
  [ "$got" = "$want" ] || die "checksum mismatch for $(basename "$file")
       expected $want
       got      $got
       The download is corrupt or has been tampered with. Nothing was installed."
  note "sha256 verified"
}

download() {
  local url="$1" file="$2" digest="${3:-}"
  info "Downloading $(basename "$url")"
  curl -fsSL "$url" -o "$file"
  verify_sha256 "$file" "$digest"
}

# --- Framework and arch ----------------------------------------------------

# Fcitx5 unless the user asked for IBus. Not a per-desktop guess any more: the
# choice is about which shell Funput is better on, and the desktop only changes
# how much session wiring the user still owes — which is what the note below is
# for. KDE already talks to Fcitx5, so it gets no note.
detect_framework() {
  [ -n "$FRAMEWORK" ] && return 0
  FRAMEWORK="fcitx5"
  case " ${XDG_CURRENT_DESKTOP:-} " in
    *KDE*|*plasma*|*Plasma*) ;;
    *) warn "this session (${XDG_CURRENT_DESKTOP:-unknown}) most likely runs IBus, so Fcitx5 will not
         receive keys until you set the session variables printed at the end and log out.
         Prefer to stay on IBus? Re-run with --ibus." ;;
  esac
}

# The arch label used in the tarball / .deb names (amd64 / arm64).
user_pkg_arch() {
  case "$(uname -m)" in
    x86_64)        echo amd64 ;;
    aarch64|arm64) echo arm64 ;;
    *) die "unsupported CPU architecture: $(uname -m)" ;;
  esac
}

# --- User-local (--user) ---------------------------------------------------

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
    *) warn "$1 is not on PATH — add it so \"funput-settings\" launches from the app menu." ;;
  esac
}

# Front half of both --user flows: resolve the framework's portable tarball,
# download it, verify it, unpack it. Sets TREE_DIR to the unpacked tree; the
# caller owns $tmp and its cleanup.
TREE_DIR=""
fetch_user_tree() {
  local fw="$1" tmp="$2" pkg_arch pattern url digest file
  pkg_arch="$(user_pkg_arch)"
  pattern="funput-${fw}-.*-${pkg_arch}-linux\\.tar\\.gz"
  info "Looking up the ${fw} tarball (${pkg_arch}) in ${REPO} ${VERSION}"
  local found; found="$(resolve_asset "$pattern")" || exit 1
  [ -n "$found" ] || die "no funput-${fw}-*-${pkg_arch}-linux.tar.gz in the ${VERSION} release"
  url="${found%%$'\t'*}"; digest="${found#*$'\t'}"
  file="$tmp/$(basename "$url")"
  download "$url" "$file" "$digest"
  info "Extracting"
  tar -xzf "$file" -C "$tmp"
  TREE_DIR="$(find "$tmp" -maxdepth 1 -type d -name "funput-${fw}-*-linux" | head -n1)"
  [ -n "$TREE_DIR" ] || die "unexpected tarball layout (no funput-${fw}-*-linux/ directory)"
}

# No-sudo, user-local IBus install. The engine binary is relocatable (its RPATH is
# $ORIGIN, so it finds libfunput_ffi.so beside it), and ibus-daemon scans
# $XDG_DATA_HOME/ibus/component — so laying the portable tarball into ~/.local and
# generating a component manifest that points <exec> at the real $HOME path is all
# it takes. No root, no package manager, no session env var.
user_install_ibus() {
  # lib/ and bin/ can live anywhere (the component <exec> is an absolute path); the
  # component XML MUST land under the ibus-scanned $XDG_DATA_HOME/ibus/component.
  local data_home="${XDG_DATA_HOME:-$HOME/.local/share}"
  local lib_dir="$HOME/.local/lib/funput"
  local bin_dir="$HOME/.local/bin"

  have ibus || warn "'ibus' is not on PATH — install the IBus daemon (needs your package manager / sudo) before the engine can run."
  if [ "$DRY_RUN" -eq 1 ]; then
    note "would install into $lib_dir, $bin_dir and $data_home/ibus/component"
    return 0
  fi

  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  fetch_user_tree ibus "$tmp"
  local src="$TREE_DIR"

  info "Installing into $HOME/.local"
  mkdir -p "$lib_dir" "$bin_dir" "$data_home/ibus/component"
  install -m 0755 "$src/lib/funput/ibus-engine-funput" "$lib_dir/"
  install -m 0644 "$src/lib/funput/libfunput_ffi.so"   "$lib_dir/"
  # ibus-daemon launches <exec> by absolute path, so point it at our real location.
  sed "s|@IBUS_ENGINE_PATH@|$lib_dir/ibus-engine-funput|g" \
    "$src/share/ibus/component/funput.xml.in" \
    | write_file "$data_home/ibus/component/funput.xml"
  place_common_assets "$src" "$data_home" "$bin_dir"
  ibus restart >/dev/null 2>&1 || true

  echo
  info "Installed (no sudo) into $HOME/.local"
  path_note "$bin_dir"
  echo "Next steps:"
  echo "  1. ibus restart            # if the engine is not listed yet"
  echo "  2. Settings → Keyboard → Input Sources → + → Vietnamese → Funput"
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  echo
  # No auto-updater on Linux yet — re-running fetches the latest release and
  # overwrites in place (then ibus restart loads the new engine).
  note "To update later: re-run this installer (it takes the latest release)."
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
  warn "could not find the system Fcitx5 addon dir; guessing /usr/lib/${triplet}/fcitx5."
  echo "/usr/lib/${triplet}/fcitx5"
}

# No-sudo, user-local Fcitx5 install. Config (.conf) is XDG-scanned like IBus, but
# the addon .so is NOT — Fcitx5 searches only its addon dirs, so we point it at
# ~/.local/lib/fcitx5 via FCITX_ADDON_DIRS in ~/.config/environment.d. That var
# OVERRIDES the default, so we re-list the system addon dir too, and it only takes
# effect after a re-login.
user_install_fcitx5() {
  local data_home="${XDG_DATA_HOME:-$HOME/.local/share}"
  local lib_dir="$HOME/.local/lib/fcitx5"
  local bin_dir="$HOME/.local/bin"
  local envd="${XDG_CONFIG_HOME:-$HOME/.config}/environment.d"

  have fcitx5 || warn "'fcitx5' is not on PATH — install the Fcitx5 daemon (needs your package manager / sudo) before the engine can run."
  if [ "$DRY_RUN" -eq 1 ]; then
    note "would install into $lib_dir, $bin_dir and $data_home/fcitx5"
    note "would write $envd/funput.conf (FCITX_ADDON_DIRS)"
    return 0
  fi

  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  fetch_user_tree fcitx5 "$tmp"
  local src="$TREE_DIR"
  local pkg_arch; pkg_arch="$(user_pkg_arch)"

  info "Installing into $HOME/.local"
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
  mkdir -p "$envd"
  printf 'FCITX_ADDON_DIRS=%s:%s\n' "$sys_addon" "$lib_dir" | write_file "$envd/funput.conf"

  echo
  info "Installed (no sudo) into $HOME/.local"
  path_note "$bin_dir"
  echo "IMPORTANT — Fcitx5 loads the addon .so only after the session env updates:"
  echo "  Wrote $envd/funput.conf → FCITX_ADDON_DIRS=$sys_addon:$lib_dir"
  echo "  1. Log out and back in (applies environment.d), OR for a quick test now:"
  echo "       FCITX_ADDON_DIRS=$sys_addon:$lib_dir fcitx5 -r -d"
  case " ${XDG_CURRENT_DESKTOP:-} " in
    # On KDE that quick test is not available: KWin starts Fcitx5 itself and hands
    # it a socket a replacement process cannot inherit, so -r leaves Wayland
    # clients with no input method until the next login.
    *KDE*|*plasma*|*Plasma*)
      echo "       ^ not on KDE: KWin owns the Fcitx5 socket, so log out instead." ;;
  esac
  echo "  2. fcitx5-configtool → + → add Funput (Vietnamese group)"
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  fcitx5_session_hint
  echo
  note "To update later: re-run this installer (it takes the latest release)."
}

# Funput is an Fcitx5 engine; apps only reach it if the *session* talks to Fcitx5.
# Funput must not write any of this — these variables belong to the session.
#
# What to set depends on the session type first and the desktop second, which is
# why this reads $XDG_SESSION_TYPE rather than printing one block for everyone:
# the classic GTK+QT+XMODIFIERS trio is right on X11 and wrong on Wayland, where
# GTK 3/4 reach the compositor through text-input-v3 on their own and setting the
# trio globally under KWin makes the candidate window blink.
#   https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland
#   https://fcitx-im.org/wiki/Setup_Fcitx_5
fcitx5_session_hint() {
  echo "  Session variables — set them, then log out and back in:"
  if [ "${XDG_SESSION_TYPE:-}" = "x11" ]; then
    echo "    X11 session — the classic three:"
    echo "           XMODIFIERS=@im=fcitx"
    echo "           GTK_IM_MODULE=fcitx"
    echo "           QT_IM_MODULE=fcitx"
    if [ "${FAMILY:-}" = "debian" ]; then
      echo "      On Debian/Ubuntu, \"im-config -n fcitx5\" writes exactly this and"
      echo "      starts the daemon for you — prefer it over editing files by hand."
    fi
  else
    case " ${XDG_CURRENT_DESKTOP:-} " in
      *KDE*|*plasma*|*Plasma*)
        echo "    KDE Plasma (Wayland) — one variable only:"
        echo "           XMODIFIERS=@im=fcitx"
        echo "      Do NOT set GTK_IM_MODULE / QT_IM_MODULE / SDL_IM_MODULE globally:"
        echo "      KWin blinks the candidate window when you do."
        echo "      Once Fcitx5 is started from System Settings → Virtual keyboard, do"
        echo "      not restart it (tray menu or fcitx5 -r): KWin hands it a socket that"
        echo "      a restarted process cannot reuse. Log out instead." ;;
      *)
        echo "    GNOME / sway / other Wayland compositors:"
        echo "           XMODIFIERS=@im=fcitx"
        echo "           QT_IM_MODULE=fcitx              # Qt 5, and Qt older than 6.8.2"
        echo "           QT_IM_MODULES=wayland;fcitx     # Qt 6.8.2+, replaces the line above"
        echo "      Leave GTK_IM_MODULE unset — GTK 3/4 use Wayland text-input-v3 directly." ;;
    esac
  fi
  echo "    Put them in ~/.config/environment.d/fcitx5.conf (read by GDM and by"
  echo "    Plasma 5.22+), or in ~/.bash_profile, which every display manager and a"
  echo "    TTY login read. Upstream notes environment.d may need a reboot to apply."
  echo "  https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland"
}

# What to do once the package is on disk. Shared by the repository path and the
# --no-repo one, which install the same package by different routes.
post_install_hint() {
  echo
  info "Installed. Next steps:"
  if [ "$FRAMEWORK" = "fcitx5" ]; then
    echo "  1. Make sure Fcitx5 runs in your session, and starts with it:"
    # Only report the daemon state when we can actually check it; a missing pgrep
    # must not be read as "not running".
    if have pgrep; then
      if pgrep -x fcitx5 >/dev/null 2>&1; then
        echo "       fcitx5 is running now."
      else
        echo "       fcitx5 is NOT running — start it with \"fcitx5 -d\" to test."
      fi
    fi
    echo "       KDE: System Settings → Virtual keyboard → Fcitx 5."
    echo "       Others: enable the Fcitx 5 autostart entry (or copy its .desktop"
    echo "       into ~/.config/autostart)."
    echo "  2. fcitx5-configtool       # + → add Funput (Vietnamese group)"
    echo "  3. Set the session variables below, then log out and back in."
    fcitx5_session_hint
  else
    echo "  1. ibus restart            # load the newly registered engine"
    echo "  2. Settings → Keyboard → Input Sources → + → Vietnamese → Funput"
  fi
  echo "  Open \"Funput\" from the app menu to switch Telex/VNI."
  if [ "$USE_REPO" -eq 0 ]; then
    echo
    note "Installed from a release asset, so this will not upgrade itself."
    note "For automatic updates, re-run without --version/--no-repo to add ${REPO_URL}."
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
  info "Adding ${REPO_URL} (apt)"
  run $SUDO install -d /usr/share/keyrings
  fetch_to_root "${REPO_URL}/funput.asc" "$keyring"
  printf 'Types: deb\nURIs: %s/deb\nSuites: ./\nSigned-By: %s\n' \
    "$REPO_URL" "$keyring" | write_root /etc/apt/sources.list.d/funput.sources
  run $SUDO apt-get update
}

# Fedora/RHEL. Written straight into /etc/yum.repos.d rather than through
# `dnf config-manager --add-repo`, which dnf5 (Fedora 41+) removed; a plain file
# drop works on both dnf4 and dnf5. The key is not imported here — funput.repo
# carries `gpgkey=` and dnf fetches it on first install.
repo_add_fedora() {
  info "Adding ${REPO_URL} (dnf)"
  fetch_to_root "${REPO_URL}/funput.repo" /etc/yum.repos.d/funput.repo
}

# openSUSE. zypper wants the key in the rpm database up front, unlike dnf.
repo_add_suse() {
  info "Adding ${REPO_URL} (zypper)"
  run $SUDO rpm --import "${REPO_URL}/funput.asc"
  run $SUDO zypper --non-interactive addrepo -gf "${REPO_URL}/rpm/" funput
  run $SUDO zypper --non-interactive refresh
}

# Arch. Two steps no other package manager needs: pacman keeps its own keyring,
# so the key has to be imported and locally signed before any signature verifies,
# and the repo section goes into pacman.conf itself rather than a drop-in file.
# x86_64 only, which is all Arch officially supports.
repo_add_arch() {
  [ "$(uname -m)" = "x86_64" ] || die "the Funput pacman repo is x86_64 only (Arch Linux ARM is a separate distribution). Try --user."
  info "Adding ${REPO_URL} (pacman)"
  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  run curl -fsSL "${REPO_URL}/funput.asc" -o "$tmp/funput.asc"
  run $SUDO pacman-key --add "$tmp/funput.asc"
  # Local signing is what promotes the key from "known" to "trusted"; without it
  # pacman reports every package as signed by an unknown key.
  run $SUDO pacman-key --lsign-key hello@funput.app

  # Appended, not written: pacman.conf is one file holding every repo, and the
  # order of sections is the priority order. Skip if it is already there so
  # re-running does not stack duplicate sections.
  if ! grep -q '^\[funput\]' /etc/pacman.conf; then
    # shellcheck disable=SC2016  # $arch is pacman's own placeholder, not a shell variable
    printf '\n[funput]\nServer = %s/arch/$arch\n' "$REPO_URL" | append_root /etc/pacman.conf
  fi
  run $SUDO pacman -Sy
}

# Install (or upgrade to) the package for the chosen framework from the repo.
repo_install() {
  info "Installing ${PACKAGE} from ${REPO_URL}"
  case "$FAMILY" in
    debian) run $SUDO apt-get install -y "$PACKAGE" ;;
    fedora) run $SUDO dnf install -y "$PACKAGE" ;;
    suse)   run $SUDO zypper --non-interactive install "$PACKAGE" ;;
    arch)   run $SUDO pacman -S --noconfirm "$PACKAGE" ;;
  esac
}

# --- Plan ------------------------------------------------------------------
# Printed before anything is touched. A `curl … | bash` user cannot review the
# script they just piped into a shell; they can review four lines of plan.
print_plan() {
  info "Funput installer"
  if [ "$USER_INSTALL" -eq 1 ]; then
    note "framework  ${FRAMEWORK}  (portable tarball)"
    note "target     $HOME/.local  (no sudo, no package manager)"
    note "source     GitHub Releases, ${VERSION}  (re-run this script to update)"
  else
    note "framework  ${FRAMEWORK}  (package: ${PACKAGE})"
    note "system     ${DISTRO_NAME:-${DISTRO_ID:-unknown}}  (${FAMILY}, $(uname -m))"
    if [ "$USE_REPO" -eq 1 ]; then
      note "source     ${REPO_URL}  (signed; automatic updates afterwards)"
    else
      note "source     GitHub Releases, ${VERSION}  (no automatic updates)"
    fi
    note "privileges ${SUDO:-none (already root)}"
  fi
  [ "$DRY_RUN" -eq 1 ] && info "Dry run — the commands below are printed, not executed"
  echo
}

# --- Arguments -------------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --fcitx5)  FRAMEWORK="fcitx5" ;;
    --ibus)    FRAMEWORK="ibus" ;;
    --user)    USER_INSTALL=1 ;;
    --no-repo) USE_REPO=0 ;;
    --dry-run) DRY_RUN=1 ;;
    # Pinning a version means leaving the repository behind: it is rebuilt from
    # the current release each time and serves nothing else.
    --version)
      shift
      [ $# -gt 0 ] || die "--version needs a release tag, e.g. --version v1.2026.28"
      VERSION="$1"; USE_REPO=0 ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
  shift
done

have curl || die "curl is required but was not found. Install it and re-run."

# --- No-sudo path ----------------------------------------------------------
# User-local install into ~/.local, then done — it needs none of the distro-family
# or package-manager logic below.
if [ "$USER_INSTALL" -eq 1 ]; then
  have tar || die "tar is required but was not found. Install it and re-run."
  [ "$(id -u)" -ne 0 ] || warn "running --user as root installs into /root/.local, which no desktop session reads."
  detect_framework
  FAMILY=""; SUDO=""
  print_plan
  case "$FRAMEWORK" in
    fcitx5) user_install_fcitx5 ;;
    ibus)   user_install_ibus ;;
    *)      die "unknown framework: $FRAMEWORK" ;;
  esac
  exit 0
fi

# --- Distro family ---------------------------------------------------------
[ -r /etc/os-release ] || die "cannot read /etc/os-release; unsupported system"

# Read one field of /etc/os-release. In a subshell on purpose: sourcing that file
# into this one overwrites any variable sharing a name with one of its fields, and
# VERSION is exactly that — the distro's version silently replaced this script's
# release tag, so --version and --no-repo resolved a nonexistent release.
os_release() {
  # shellcheck disable=SC1091  # a system file, not part of this repository
  ( set +u; . /etc/os-release; printf '%s' "${!1:-}" )
}
DISTRO_ID="$(os_release ID)"
DISTRO_LIKE="$(os_release ID_LIKE)"
DISTRO_NAME="$(os_release PRETTY_NAME)"

# Match on ID first, then ID_LIKE so derivatives (Linux Mint, Pop!_OS, Nobara,
# openSUSE variants, Manjaro/EndeavourOS) resolve to the right family.
case " ${DISTRO_ID} ${DISTRO_LIKE} " in
  *" debian "*|*" ubuntu "*) FAMILY="debian" ;;
  *" fedora "*|*" rhel "*)   FAMILY="fedora" ;;
  *" suse "*|*" opensuse "*) FAMILY="suse" ;;
  *" arch "*)                FAMILY="arch" ;;
  *) die "unsupported distro (ID=${DISTRO_ID:-?}, ID_LIKE=${DISTRO_LIKE:-?}). Build from source: platforms/linux/build.sh" ;;
esac

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  have sudo || die "this install needs root and sudo was not found. Re-run as root, or use --user."
  SUDO="sudo"
fi

# Arch has no release asset: it is a rolling source distro, so the .deb/.rpm
# builds have nothing to offer it. What repo.funput.app serves instead is a
# pacman repository of the same three packages, under the same key. Handled
# entirely by repo_add_arch below, so it falls through with the others.

detect_framework
PACKAGE="funput"
[ "$FRAMEWORK" = "ibus" ] && PACKAGE="funput-ibus"
print_plan

# --- Repository path (the default) -----------------------------------------
# Everything below this block is the --no-repo fallback: one versioned asset,
# updated only by re-running the script.
if [ "$USE_REPO" -eq 1 ]; then
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
    *) die "unsupported CPU architecture: $MACHINE" ;;
  esac
  PATTERN="${PACKAGE}_.*_${PKG_ARCH}\.deb"
else
  # fedora + suse → .rpm
  FORMAT="rpm"
  case "$MACHINE" in
    x86_64) PKG_ARCH="x86_64" ;;
    aarch64|arm64) PKG_ARCH="aarch64" ;;
    *) die "unsupported CPU architecture: $MACHINE" ;;
  esac
  if [ "$FRAMEWORK" = "ibus" ]; then
    PATTERN="funput-ibus-.*\.${PKG_ARCH}\.rpm"
  else
    # funput- followed by a digit = version, to not match funput-ibus-*.
    PATTERN="funput-[0-9].*\.${PKG_ARCH}\.rpm"
  fi
fi

# --- Resolve + download the release asset ----------------------------------
info "Looking up the ${FRAMEWORK} ${FORMAT} (${PKG_ARCH}) in ${REPO} ${VERSION}"
FOUND="$(resolve_asset "$PATTERN")" || exit 1
[ -n "$FOUND" ] || die "no matching asset (${PATTERN}) in the ${VERSION} release"
URL="${FOUND%%$'\t'*}"; DIGEST="${FOUND#*$'\t'}"

if [ "$DRY_RUN" -eq 1 ]; then
  note "would download $(basename "$URL") and verify its sha256"
  note "would install it with ${FAMILY}'s package manager"
  post_install_hint
  exit 0
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FILE="$TMP/$(basename "$URL")"
download "$URL" "$FILE" "$DIGEST"

# --- Install ---------------------------------------------------------------
info "Installing $(basename "$FILE")"
case "$FAMILY" in
  debian) $SUDO apt-get install -y "$FILE" ;;
  fedora) $SUDO dnf install -y "$FILE" ;;
  # Release assets are unsigned (only the repository signs); zypper stops on that
  # unless told, while apt and dnf take a local file as it is.
  suse)   $SUDO zypper --non-interactive install --allow-unsigned-rpm "$FILE" ;;
esac

post_install_hint
