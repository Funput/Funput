# Production-like Funput.exe build for local Windows testing.
# Mirrors CI (app/.github/workflows/build-windows.yml): `cargo build --release`
# with the crate's release profile (opt-level=s, LTO=fat, strip), then stages
# Funput-<version>.exe + Funput.exe + .sha256 under build/release/.
#
# Usage (Windows PowerShell 5.1 or PowerShell 7+), from platforms/windows:
#   .\scripts\build-release.ps1
#   pwsh -File .\scripts\build-release.ps1
#   .\scripts\build-release.ps1 -Version 1.2026.1
#   .\scripts\build-release.ps1 -Target x86_64-pc-windows-msvc
#
# Prefer MSVC on Windows (same as windows-latest CI). Install the "Desktop
# development with C++" workload if rustc defaults to *-msvc.

[CmdletBinding()]
param(
    [string]$Version = "",
    [string]$Target = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if (-not $Version) {
    $line = Select-String -Path "Cargo.toml" -Pattern '^version = "(.+)"' | Select-Object -First 1
    if ($line) { $Version = $line.Matches[0].Groups[1].Value }
}
if (-not $Version) { $Version = "dev" }

$targetNote = if ($Target) { ", target=$Target" } else { "" }
Write-Host "Building Funput $Version (release$targetNote)..."

$cargoArgs = @("build", "--release")
if ($Target) {
    rustup target add $Target 2>$null | Out-Null
    $cargoArgs += @("--target", $Target)
}

& cargo @cargoArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Target) {
    $Exe = Join-Path $Root "target\$Target\release\funput.exe"
} else {
    $Exe = Join-Path $Root "target\release\funput.exe"
}

if (-not (Test-Path $Exe)) {
    Write-Error "Expected binary missing: $Exe"
}

$Out = Join-Path $Root "build\release"
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$Dest = Join-Path $Out "Funput-$Version.exe"
Copy-Item -Force $Exe $Dest
Copy-Item -Force $Exe (Join-Path $Out "Funput.exe")

$hash = (Get-FileHash -Algorithm SHA256 $Dest).Hash.ToLowerInvariant()
Set-Content -Path "$Dest.sha256" -Value $hash -NoNewline

$bytes = (Get-Item $Dest).Length
Write-Host "Staged: $Dest ($bytes bytes)"
Write-Host "Also:   $(Join-Path $Out 'Funput.exe') (stable name for local/autostart)"
Write-Host "SHA256:  $hash"
Write-Host "Run Funput.exe (or Funput-$Version.exe once — it normalizes to Funput.exe)."
