param(
    [string]$ApiJar = "$env:USERPROFILE/.m2/repository/org/spigotmc/spigot-api/26.3-R0.1-SNAPSHOT/spigot-api-26.3-R0.1-SNAPSHOT.jar",
    [string]$LegacyApiJar = "$env:USERPROFILE/.m2/repository/org/spigotmc/spigot-api/1.19.4-R0.1-SNAPSHOT/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath "$PSScriptRoot/../..").Path
$verificationRoot = Join-Path $projectRoot 'run/verification-0.9.0'
$classes = Join-Path $verificationRoot 'harness-classes'
$pluginJar = Join-Path $projectRoot 'target/survivalfriend-0.9.0-BETA.jar'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& javac --release 17 -cp "$ApiJar;$pluginJar" -d $classes "$PSScriptRoot/ReleaseVerification.java"
if ($LASTEXITCODE -ne 0) { throw 'Harness compilation failed' }
& javac --release 17 -cp $LegacyApiJar -d $classes "$PSScriptRoot/LegacyView.java"
if ($LASTEXITCODE -ne 0) { throw 'Legacy view compilation failed' }
Set-Content -LiteralPath "$classes/plugin.yml" -Value @'
name: ReleaseVerification
version: 0.9.0
main: verification.ReleaseVerification
api-version: '1.19'
folia-supported: true
depend: [SurvivalFriend]
'@
$harnessJar = Join-Path $verificationRoot 'ReleaseVerification.jar'
& jar "-J-Djava.io.tmpdir=$verificationRoot" --create --file $harnessJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Harness packaging failed' }
Write-Output $harnessJar
