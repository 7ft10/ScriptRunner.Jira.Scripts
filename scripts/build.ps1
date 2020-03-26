Param(
    [Parameter(Mandatory = $true)]
    [bool]$IsReleaseBuild
)

Write-Host "Starting ($(if ($IsReleaseBuild) { "Release" } else { "Debug" })) Build..."

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
    Write-Progress -Activity "Building" -Status "Cleaning" -percentComplete 0
    Get-ChildItem -Path './obj/release' -Include * -File -Recurse | ForEach-Object { $_.Delete() }

    Copy-Item -Path './src/*' -Destination './obj/release/' -Force -Recurse

    $files = Get-ChildItem -Path './obj/release' -Include *.groovy -File -Recurse
    $i = 0
    $files | ForEach-Object {
        Write-Progress `
            -Activity "Building" `
            -Status "Joining dot line breaks in $($_.BaseName).$($_.Extension)" -percentComplete ($i / $files.Count * 100)
        & "./scripts/Minify-Groovy.ps1" -file $_
    }
} else {
    Write-Progress -Activity "Building" -Status "Cleaning" -percentComplete 0
    Get-ChildItem -Path './obj/debug' -Include * -File -Recurse | ForEach-Object { $_.Delete() }

    Copy-Item -Path './src/*' -Destination './obj/debug/' -Force -Recurse
}

Write-Host "Build Completed."