Param(
    [Parameter(Mandatory = $true)]
    [System.IO.FileInfo]$file
)

$fileSB = New-Object System.Text.StringBuilder

## Remove comments and blank lines
(Get-Content $file) `
    | ForEach-Object { $_.Trim() `
        -Replace "(?m)(?:^//.*)", "" `
        -Replace "(?m)(?:/\*(.*)/*\/)", "" `
        -Replace '\s+(?=((\\[\\"]|[^\\"])*"(\\[\\"]|[^\\"])*")*(\\[\\"]|[^\\"])*$)', " " } `
    | Where-Object { $_.Trim() -ne "" } `
    | ForEach-Object { [void]$fileSB.AppendLine($_.Trim()) }

$newFile = $fileSB.ToString()
@(
    ( "`r`n.", "." ),
    ( ".`r`n", "." ),
    ( "`r`n)", " )" ),
    ( "`r`n[", " [" ),
    ( "`r`n]", " ]" ),
    ( "->`r`n", "-> "),
    ( "`r`n&&", " &&" ),
    ( "&&`r`n", "&& " ),
    ( "`r`n||", " ||" ),
    ( "||`r`n", "|| " )
    ( "(`r`n", "( "),
    ( ",`r`n", ", "),
    ( "[`r`n", "[ "),
    ( ")`r`n", "); "),
    ( "`r`n}`r`n}", " } }" ),
    ( "}`r`n", "}; "),
    ( "`r`n}", " }"),
    ( "{`r`n", "{ " ),
    ( "`r`n", "; " )
) | ForEach-Object { $newFile = $newFile.Replace($_[0], $_[1]) }
##$newFile = $newFile -Replace "[\)]\s*`r`n", "); "
Set-Content -Path $file -Value $newFile.Trim()