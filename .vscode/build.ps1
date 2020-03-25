Param(
    [Parameter(Mandatory = $true)]
    [bool]$IsReleaseBuild
)

if (!(Test-Path -Path './obj/')) {
    New-Item -ItemType directory -Path './obj/'
}
if (!(Test-Path -Path './obj/release')) {
    New-Item -ItemType directory -Path './obj/release'
}
if (!(Test-Path -Path './obj/debug')) {
    New-Item -ItemType directory -Path './obj/debug'
}

if ($IsReleaseBuild) {
    Write-Host "Starting Release Build..."
    Write-Progress -Activity "Building" -Status "Cleaning" -percentComplete 0
    Get-ChildItem -Path './obj/release' -Include * -File -Recurse | ForEach-Object { $_.Delete() }

    Copy-Item -Path './src/*' -Destination './obj/release/' -Force -Recurse

    $files = Get-ChildItem -Path './obj/release' -Include *.groovy -File -Recurse
    $i = 0
    $files | ForEach-Object {
        $file = $_
        Write-Progress -Activity "Building" -Status "Removing comments from $($file.BaseName).$($file.Extension)" -percentComplete ($i++ / $files.Count * 100)
        $content = (Get-Content $file) | % { $_ -Replace "(?m)(?:^//.*)", "" } | % { $_ -Replace "(?m)(?:/\*(.*)/*\/)", "" } | ? {$_.Trim() -ne "" }
        Set-Content -Path $file -Value $content.Trim()
        Write-Progress -Activity "Building" -Status "Joining dot line breaks in $($file.BaseName).$($file.Extension)" -percentComplete ($i / $files.Count * 100)
        $content = ((Get-Content $file -Raw) -Replace "`r`n\.", ".") -Replace "`r`n}", " }"
        Set-Content -Path $file -Value $content.Trim()
    }
} else {
    Write-Host "Starting Debug Build..."
    Write-Progress -Activity "Building" -Status "Cleaning" -percentComplete 0
    Get-ChildItem -Path './obj/debug' -Include * -File -Recurse | ForEach-Object { $_.Delete() }

    Copy-Item -Path './src/*' -Destination './obj/debug/' -Force -Recurse

    $files = Get-ChildItem -Path './obj/debug' -Include *.groovy -File -Recurse
}

Write-Host "Build Completed."