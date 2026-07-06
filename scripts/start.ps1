[CmdletBinding()]
param (
    [Parameter(Mandatory = $false)]
    [string[]]$JvmArgs = @()
)

# Find the server-core jar
$JarPath = Get-ChildItem server-core-*.jar
if (-not (Test-Path $JarPath)) {
    throw "Application jar file not found"
}

# Find Java Executable
$JavaExe = $null
if ($env:JAVA_HOME) {
    $JavaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $JavaExe = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
}
if (-not $JavaExe -or -not (Test-Path $JavaExe)) {
    throw "Java executable could not be found. Please ensure Java is installed and JAVA_HOME or PATH is configured."
}

# Start the application
Write-Verbose "Using Java: $JavaExe"
Write-Verbose "Starting application..."

# Combine arguments cleanly, omitting extra spaces if $JvmArgs is empty
Start-Process -FilePath $JavaExe -ArgumentList (("-jar", "`"$JarPath`"") + $JvmArgs) -NoNewWindow -Wait