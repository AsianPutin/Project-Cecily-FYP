Add-Type -AssemblyName System.Drawing

$resDir = "C:\Users\Dingus\Documents\I-CATS STUFF BSE COURSE\Semester 7\IT Project 2 [BSE 7024]\FYP SHIT old\app\src\main\res"
$fgColorPath = Join-Path $resDir "drawable\ic_launcher_foreground.png"
$fgMonoPath = Join-Path $resDir "drawable\ic_launcher_monochrome_fg.png"

$densities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($folder in $densities.Keys) {
    $size = $densities[$folder]
    $folderPath = Join-Path $resDir $folder

    # 1. Generate Colored Launcher Icon
    $colorDest = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($colorDest)
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

    # Fill white background
    $g.Clear([System.Drawing.Color]::White)

    # Draw colored foreground
    $fgBmp = [System.Drawing.Bitmap]::FromFile($fgColorPath)
    $g.DrawImage($fgBmp, 0, 0, $size, $size)
    $fgBmp.Dispose()
    $g.Dispose()

    $colorDest.Save((Join-Path $folderPath "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $colorDest.Save((Join-Path $folderPath "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $colorDest.Dispose()

    # 2. Generate Monochrome Launcher Icon
    $monoDest = New-Object System.Drawing.Bitmap($size, $size)
    $g2 = [System.Drawing.Graphics]::FromImage($monoDest)
    $g2.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

    $g2.Clear([System.Drawing.Color]::White)

    $monoFgBmp = [System.Drawing.Bitmap]::FromFile($fgMonoPath)
    $g2.DrawImage($monoFgBmp, 0, 0, $size, $size)
    $monoFgBmp.Dispose()
    $g2.Dispose()

    $monoDest.Save((Join-Path $folderPath "ic_launcher_monochrome.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $monoDest.Save((Join-Path $folderPath "ic_launcher_monochrome_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $monoDest.Dispose()

    Write-Host "Updated $folder ($size x $size)"
}
