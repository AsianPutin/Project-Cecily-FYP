Add-Type -AssemblyName System.Drawing

$drawableDir = "C:\Users\Dingus\Documents\I-CATS STUFF BSE COURSE\Semester 7\IT Project 2 [BSE 7024]\FYP SHIT old\app\src\main\res\drawable"
$files = @("ic_launcher_foreground.png", "ic_launcher_monochrome_fg.png")

# Target shift: +7 pixels to the RIGHT (X = +7), Y = 0 (keep vertical Y position)
$shiftX = 7
$shiftY = 0

foreach ($file in $files) {
    $filePath = Join-Path $drawableDir $file
    if (Test-Path -LiteralPath $filePath) {
        $src = [System.Drawing.Bitmap]::FromFile($filePath)
        $dest = New-Object System.Drawing.Bitmap($src.Width, $src.Height)
        $g = [System.Drawing.Graphics]::FromImage($dest)

        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

        # Draw source image shifted right by $shiftX pixels
        $g.DrawImage($src, $shiftX, $shiftY, $src.Width, $src.Height)

        $g.Dispose()
        $src.Dispose()

        # Save back to file
        $dest.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)
        $dest.Dispose()
        Write-Host "Successfully shifted $file right by $shiftX pixels."
    }
}
