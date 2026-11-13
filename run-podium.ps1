#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Runs Podium Desktop with JavaFX module path computed from Gradle cache.
.DESCRIPTION
    Computes the JavaFX module path from Gradle's dependency cache and launches
    the application with the correct --module-path and --add-modules arguments.
#>

param(
    [switch]$NoDaemon = $true,
    [switch]$Debug = $false
)

$ErrorActionPreference = "Stop"

# Path to the project
$ProjectDir = $PSScriptRoot

# Gradle wrapper
$Gradle = Join-Path $ProjectDir "gradlew.bat"

# Get the runtime classpath for the desktopApp module
Write-Host "Resolving JavaFX module path from Gradle cache..." -ForegroundColor Cyan

$ClasspathCmd = @($Gradle, ":desktopApp:runtimeClasspath", "--no-daemon", "--configuration-cache", "-q")
if ($Debug) { $ClasspathCmd += "--info" }

# Use Start-Process to properly execute the batch file
$ProcessInfo = New-Object System.Diagnostics.ProcessStartInfo
$ProcessInfo.FileName = $Gradle
$ProcessInfo.Arguments = ($ClasspathCmd | Select-Object -Skip 1) -join " "
$ProcessInfo.RedirectStandardOutput = $true
$ProcessInfo.RedirectStandardError = $true
$ProcessInfo.UseShellExecute = $false
$ProcessInfo.CreateNoWindow = $true
$ProcessInfo.WorkingDirectory = $ProjectDir  # Set working directory

$Process = [System.Diagnostics.Process]::Start($ProcessInfo)
$ClasspathOutput = $Process.StandardOutput.ReadToEnd()
$ErrorOutput = $Process.StandardError.ReadToEnd()
$Process.WaitForExit()

if ($Process.ExitCode -ne 0) {
    Write-Error "Failed to resolve runtime classpath: $ErrorOutput"
    exit 1
}

# Extract JavaFX JAR paths from classpath
$JavaFxJars = $ClasspathOutput -split ';' | Where-Object { $_ -match 'javafx-.*\.jar$' } | ForEach-Object { $_.Trim() } | Where-Object { Test-Path $_ }

if (-not $JavaFxJars) {
    Write-Error "No JavaFX JARs found in runtime classpath. Run './gradlew :desktopApp:compileKotlin' first."
    exit 1
}

$ModulePath = $JavaFxJars -join [IO.Path]::PathSeparator

Write-Host "Found JavaFX JARs:" -ForegroundColor Green
$JavaFxJars | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }

# Build JVM args
$JvmArgs = @(
    "--module-path", $ModulePath,
    "--add-modules", "javafx.controls,javafx.fxml,javafx.media,javafx.base,javafx.graphics,javafx.web"
)

# Build Gradle command
$GradleArgs = @(":desktopApp:run")
if ($NoDaemon) { $GradleArgs += "--no-daemon" }
$GradleArgs += "--no-configuration-cache"  # Avoid cache issues with dynamic JVM args

# Set JAVA_OPTS and run
$Env:JAVA_OPTS = $JvmArgs -join " "

Write-Host "Launching Podium Desktop..." -ForegroundColor Cyan
Write-Host "JAVA_OPTS=$Env:JAVA_OPTS" -ForegroundColor Gray

& $Gradle $GradleArgs 2>&1