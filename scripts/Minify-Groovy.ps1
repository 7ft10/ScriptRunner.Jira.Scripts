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

## remove new line combination
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
) | ForEach-Object { [void]$fileSB.Replace($_[0], $_[1]) }
Set-ItemProperty $file.FullName -name IsReadOnly -value $false
Set-Content -Path $file -Value $fileSB.ToString().Trim()
Set-ItemProperty $file.FullName -name IsReadOnly -value $true