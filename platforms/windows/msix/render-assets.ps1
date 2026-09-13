# Render every MSIX logo variant from assets/logo.png.
#
# Usage, from anywhere:  .\platforms\windows\msix\render-assets.ps1
#
# Every asset is the square logo scaled to a side, centred on its canvas —
# the layout the first hand-made assets used. The Wide tile and splash screen
# are that square on a wider transparent canvas.
#
# Qualifiers live in folder names (scale-200/, targetsize-48/) so no folder
# holds more than five files; makepri reads folder and file-name qualifiers
# alike, and pack-msix.ps1 builds resources.pri from them. Without that PRI,
# Windows would load only the unqualified file and stretch it.
#
#   Icons/                   Square44x44Logo (44), StoreLogo (50)
#   Icons/scale-200/         the same at 88 and 100
#   Icons/targetsize-N/      Square44x44Logo at N, plain and altform-unplated,
#                            for the taskbar, Start list, Alt-Tab and Settings
#   Tiles/                   Square150x150Logo, Wide310x150Logo, SplashScreen
#   Tiles/scale-200/         the same at twice the size

[CmdletBinding()]
param([string]$Source = "")

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$Windows = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
if (-not $Source) { $Source = Join-Path $Windows "..\..\assets\logo.png" }
$Source = [System.IO.Path]::GetFullPath($Source)
$Assets = Join-Path $PSScriptRoot "Assets"

# Unplated and plated are the same pixels: the logo already sits on transparency.
$TargetSizes = 16, 24, 32, 48, 256

function Save-Logo([System.Drawing.Image]$Logo, [string]$Path, [int]$Width, [int]$Height, [int]$Side) {
    $canvas = New-Object System.Drawing.Bitmap $Width, $Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $attributes = New-Object System.Drawing.Imaging.ImageAttributes
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        # Mirror the edge pixels instead of sampling black past them.
        $attributes.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
        $x = [int][math]::Floor(($Width - $Side) / 2)
        $y = [int][math]::Floor(($Height - $Side) / 2)
        $dest = New-Object System.Drawing.Rectangle $x, $y, $Side, $Side
        $graphics.DrawImage($Logo, $dest, 0, 0, $Logo.Width, $Logo.Height, [System.Drawing.GraphicsUnit]::Pixel, $attributes)
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
        $canvas.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host ("{0,-60} {1}x{2}" -f $Path.Substring($Assets.Length + 1), $Width, $Height)
    }
    finally {
        $attributes.Dispose()
        $graphics.Dispose()
        $canvas.Dispose()
    }
}

# Tiles and splash at a scale factor; the square inside keeps the original ratios.
function Save-Tiles([System.Drawing.Image]$Logo, [string]$Dir, [int]$Scale) {
    Save-Logo $Logo (Join-Path $Dir "Square150x150Logo.png") (150 * $Scale) (150 * $Scale) (150 * $Scale)
    Save-Logo $Logo (Join-Path $Dir "Wide310x150Logo.png") (310 * $Scale) (150 * $Scale) (150 * $Scale)
    Save-Logo $Logo (Join-Path $Dir "SplashScreen.png") (620 * $Scale) (300 * $Scale) (256 * $Scale)
}

if (-not (Test-Path $Source)) { throw "Logo source missing: $Source" }
$logo = [System.Drawing.Image]::FromFile($Source)
try {
    if ($logo.Width -ne $logo.Height) { throw "Logo source must be square, got $($logo.Width)x$($logo.Height)" }
    # Start clean so a renamed or dropped variant cannot linger in the package.
    Get-ChildItem -Path $Assets -Recurse -Filter *.png -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem -Path $Assets -Recurse -Directory -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Where-Object { -not (Get-ChildItem $_.FullName -Force) } |
        Remove-Item -Force

    $icons = Join-Path $Assets "Icons"
    foreach ($scale in 1, 2) {
        $dir = if ($scale -eq 1) { $icons } else { Join-Path $icons "scale-200" }
        Save-Logo $logo (Join-Path $dir "Square44x44Logo.png") (44 * $scale) (44 * $scale) (44 * $scale)
        Save-Logo $logo (Join-Path $dir "StoreLogo.png") (50 * $scale) (50 * $scale) (50 * $scale)
    }
    foreach ($size in $TargetSizes) {
        $dir = Join-Path $icons "targetsize-$size"
        Save-Logo $logo (Join-Path $dir "Square44x44Logo.png") $size $size $size
        Save-Logo $logo (Join-Path $dir "Square44x44Logo.altform-unplated.png") $size $size $size
    }

    $tiles = Join-Path $Assets "Tiles"
    Save-Tiles $logo $tiles 1
    Save-Tiles $logo (Join-Path $tiles "scale-200") 2
}
finally {
    $logo.Dispose()
}
