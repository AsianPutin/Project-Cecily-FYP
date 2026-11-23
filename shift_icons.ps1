Add-Type -AssemblyName System.Drawing

$drawableDir = "C:\Users\Dingus\Documents\I-CATS STUFF BSE COURSE\Semester 7\IT Project 2 [BSE 7024]\FYP SHIT old\app\src\main\res\drawable"
$shiftY = 22 # Shift logo 22 pixels downwards on 512x512 canvas

$files = @("ic_launcher_foreground.png", "ic_launcher_monochrome_fg.png")

foreach ($file in $files) {
    $filePath = Join-Path $drawableDir $file
    if (Test-Path -LiteralPath $filePath) {
        $src = [System.Drawing.Bitmap]::FromFile($filePath)
        $dest = New-Object System.Drawing.Bitmap($src.Width, $src.Height)
        $g = [System.Drawing.Graphics]::FromImage($dest)

        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

        # Draw source image shifted down by $shiftY pixels
        $g.DrawImage($src, 0, $shiftY, $src.Width, $src.Height)

        $g.Dispose()
        $src.Dispose()

        # Save back to file
        $dest.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)
        $dest.Dispose()
        Write-Host "Successfully shifted $file down by $shiftY pixels."
    } else {
        Write-Host "File not found: $filePath"
    }
}
