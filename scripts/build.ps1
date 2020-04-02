Param(
    [Parameter(Mandatory = $true)]
    [bool]$IsReleaseBuild
)

function Show-Completed {
    param(
        [Parameter(Mandatory = $true)]
        [double]$Percentage,
        [Parameter(Mandatory = $true)]
        [string]$Activity,
        [string]$Status = "Processing"
    )
    Write-Progress -Activity $Activity -Status $Status -percentComplete $Percentage
    Write-Host "`r$([math]::floor($Percentage).ToString().PadLeft(3, " "))% Completed" -NoNewline
}
if ($IsReleaseBuild) {
    Write-Host "****************************************************************************************************" -ForegroundColor "Green"
    Write-Host "** Due to ScriptRunner's limitation of 32kb for 'Release' builds groovy scripts will be minified. **" -ForegroundColor "Green"
    Write-Host "****************************************************************************************************`r`n" -ForegroundColor "Green"
}

Write-Host "Building " -NoNewline
Write-Host "All versions + Clients " -ForegroundColor "Green" -NoNewline
Write-Host "($(if ($IsReleaseBuild) { "Release" } else { "Debug" }))" -ForegroundColor "DarkMagenta"

Show-Completed -Percentage 1 -Activity "Build" -Status "Creating Folders"

if (!(Test-Path -Path './obj/')) {
    New-Item -ItemType directory -Path './obj/' | Out-Null
}
if (!(Test-Path -Path './bin/')) {
    New-Item -ItemType directory -Path './bin/' | Out-Null
}

if ($IsReleaseBuild) {
    Show-Completed -Percentage 5 -Activity "Build" -Status "Creating Folders"
    if (!(Test-Path -Path './bin/release')) {
        New-Item -ItemType directory -Path './bin/release' | Out-Null
    }
    if (!(Test-Path -Path './obj/release')) {
        New-Item -ItemType directory -Path './obj/release' | Out-Null
    }

    Show-Completed -Percentage 10 -Activity "Build" -Status "Cleaning"
    Get-ChildItem -Path './obj/release' -Include * -File -Recurse | ForEach-Object { Set-ItemProperty $_.FullName -name IsReadOnly -value $false; $_.Delete() }

    Show-Completed -Percentage 15 -Activity "Build" -Status "Cleaning"
    Get-ChildItem -Path './obj/release' -Include * -File -Recurse | ForEach-Object { Set-ItemProperty $_.FullName -name IsReadOnly -value $false; $_.Delete() }

    Show-Completed -Percentage 20 -Activity "Copying"
    Copy-Item -Path './src/*' -Destination './obj/release/' -Force -Recurse | Out-Null

    ## Minify
    $i = 0; $files = Get-ChildItem -Path './obj/release' -Include *.groovy -File -Recurse
    $files | ForEach-Object {
        & "./scripts/Minify-Groovy.ps1" -file $_
        Show-Completed -Percentage (20 + ($i++ / ($files.Count) * 100) * 0.8) -Activity "Minifying" -Status "Joining dot line breaks in $($_.BaseName).$($_.Extension)"
    }

    Show-Completed -Percentage 90 -Activity "Releasing"
    Copy-Item -Path './obj/release/*' -Destination './bin/release/' -Force -Recurse | Out-Null
} else {
    Show-Completed -Percentage 10 -Activity "Build" -Status "Creating Folders"
    if (!(Test-Path -Path './bin/debug')) {
        New-Item -ItemType directory -Path './bin/debug' | Out-Null
    }
    if (!(Test-Path -Path './obj/debug')) {
        New-Item -ItemType directory -Path './obj/debug' | Out-Null
    }

    Show-Completed -Percentage 20 -Activity "Cleaning"
    Get-ChildItem -Path './obj/debug' -Include * -File -Recurse | ForEach-Object { Set-ItemProperty $_.FullName -name IsReadOnly -value $false; $_.Delete() }

    Show-Completed -Percentage 40 -Activity "Cleaning"
    Get-ChildItem -Path './bin/debug' -Include * -File -Recurse | ForEach-Object { Set-ItemProperty $_.FullName -name IsReadOnly -value $false; $_.Delete() }

    Show-Completed -Percentage 60 -Activity "Copying"
    Copy-Item -Path './src/*' -Destination './obj/debug/' -Force -Recurse | Out-Null

    Show-Completed -Percentage 80 -Activity "Releasing"
    Copy-Item -Path './src/*' -Destination './bin/debug/' -Force -Recurse | Out-Null
}

Show-Completed -Percentage 100 -Activity "Build" -Status "Completed"