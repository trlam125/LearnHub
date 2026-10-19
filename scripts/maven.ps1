$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
$mavenVersion = '3.9.11'
$cacheDir = Join-Path $projectDir '.tools'
$mavenDir = Join-Path $cacheDir "apache-maven-$mavenVersion"
if (-not (Test-Path (Join-Path $mavenDir '.ready'))) {
    New-Item -ItemType Directory -Force $cacheDir | Out-Null
    $archive = Join-Path $cacheDir "apache-maven-$mavenVersion-bin.zip"
    $sourceUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Write-Host "Downloading Apache Maven $mavenVersion from Maven Central..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -UseBasicParsing $sourceUrl -OutFile $archive
    $expected = (Invoke-WebRequest -UseBasicParsing "$sourceUrl.sha512").Content.Trim().Split(' ')[0].Trim()
    if ($expected -notmatch '^[a-fA-F0-9]{128}$') { throw 'Invalid Maven checksum response' }
    $actual = (Get-FileHash $archive -Algorithm SHA512).Hash
    if ($actual -ne $expected) { throw 'Maven checksum mismatch' }
    Expand-Archive -Path $archive -DestinationPath $cacheDir -Force
    New-Item -ItemType File -Force (Join-Path $mavenDir '.ready') | Out-Null
}
& (Join-Path $mavenDir 'bin\mvn.cmd') @args
exit $LASTEXITCODE
