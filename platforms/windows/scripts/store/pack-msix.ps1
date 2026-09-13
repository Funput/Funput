# Pack Funput.exe into an unsigned Store .msix (Microsoft re-signs on ingest).
#
# Usage, from platforms/windows:
#   .\scripts\store\pack-msix.ps1 -Exe target\release\funput.exe -Version 1.2026.66.0 `
#       -IdentityName <from Partner Center> -Publisher "CN=..." `
#       -PublisherDisplayName Funput -OutDir build\msix
#
# -DisplayName is the package's Properties/DisplayName, which Partner Center
# checks against the app's reserved names. The Start menu tile keeps "Funput".

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Exe,
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$IdentityName,
    [Parameter(Mandatory = $true)][string]$Publisher,
    [Parameter(Mandatory = $true)][string]$PublisherDisplayName,
    [string]$DisplayName = "Funput",
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
# platforms/windows, two levels above scripts/store/.
$Root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$Exe = [System.IO.Path]::GetFullPath($Exe)
if ($OutDir) { $OutDir = [System.IO.Path]::GetFullPath($OutDir) }
if (-not $OutDir) { $OutDir = Join-Path $Root "build\msix" }
Set-Location $Root

function Find-MakeAppx {
    $roots = @(
        "${env:ProgramFiles(x86)}\Windows Kits\10\bin",
        "${env:ProgramFiles}\Windows Kits\10\bin"
    ) | Where-Object { Test-Path $_ }
    $found = Get-ChildItem -Path $roots -Filter makeappx.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Directory.Name -eq "x64" } |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if (-not $found) {
        throw "makeappx.exe not found. Install the Windows SDK (windows-latest has it)."
    }
    $found.FullName
}

function Assert-Token([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { throw "$Name is empty." }
    if ($Value -match '[<>&"]') { throw "$Name contains XML-special characters: $Value" }
}

Assert-Token "IdentityName" $IdentityName
Assert-Token "Publisher" $Publisher
Assert-Token "PublisherDisplayName" $PublisherDisplayName
Assert-Token "DisplayName" $DisplayName
# The fourth part is reserved for the Store and must be 0 (msix_version.py).
if ($Version -notmatch "^\d+\.\d+\.\d+\.0$") {
    throw "Version must be four numeric parts ending in .0 (e.g. 1.2026.66.0), got $Version"
}
if (-not (Test-Path $Exe)) { throw "Executable missing: $Exe" }

$MakeAppx = Find-MakeAppx
$Stage = Join-Path $env:TEMP ("funput-msix-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $Stage | Out-Null
try {
    Copy-Item -Force $Exe (Join-Path $Stage "Funput.exe")
    # Assets\Icons and Assets\Tiles keep their layout; the manifest names them so.
    Copy-Item -Recurse -Force (Join-Path $Root "msix\Assets") (Join-Path $Stage "Assets")
    $manifest = [System.IO.File]::ReadAllText((Join-Path $Root "msix\AppxManifest.xml.template"))
    $manifest = $manifest.Replace("__IDENTITY_NAME__", $IdentityName)
    $manifest = $manifest.Replace("__PUBLISHER__", $Publisher)
    # __DISPLAY_NAME__ is a substring of __PUBLISHER_DISPLAY_NAME__: keep this order.
    $manifest = $manifest.Replace("__PUBLISHER_DISPLAY_NAME__", $PublisherDisplayName)
    $manifest = $manifest.Replace("__DISPLAY_NAME__", $DisplayName)
    $manifest = $manifest.Replace("__VERSION__", $Version)
    if ($manifest -match "__[A-Z_]+__") {
        throw "Manifest still has unsubstituted placeholders."
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText((Join-Path $Stage "AppxManifest.xml"), $manifest, $utf8)

    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $Package = Join-Path $OutDir "Funput-$Version.msix"
    if (Test-Path $Package) { Remove-Item -Force $Package }
    & $MakeAppx pack /d $Stage /p $Package /o
    if ($LASTEXITCODE -ne 0) { throw "makeappx pack failed with exit $LASTEXITCODE" }
    if (-not (Test-Path $Package)) { throw "makeappx reported success but $Package is missing" }
    Write-Host "Packed $Package"
    $Package
}
finally {
    Remove-Item -Recurse -Force $Stage -ErrorAction SilentlyContinue
}
