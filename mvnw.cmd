@REM ----------------------------------------------------------------------------
@REM Minimal Maven Wrapper for Windows.
@REM Downloads Apache Maven (version pinned in .mvn\wrapper\maven-wrapper.properties)
@REM to %USERPROFILE%\.m2\wrapper on first use, then invokes it with the passed args.
@REM ----------------------------------------------------------------------------
@echo off
setlocal EnableDelayedExpansion

set "MVN_VERSION=3.9.9"
set "WRAPPER_DIR=%USERPROFILE%\.m2\wrapper"
set "MVN_HOME=%WRAPPER_DIR%\apache-maven-%MVN_VERSION%"
set "MVN_ZIP=%WRAPPER_DIR%\apache-maven-%MVN_VERSION%-bin.zip"
set "MVN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip"

if not exist "%MVN_HOME%\bin\mvn.cmd" (
    if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
    echo Downloading Apache Maven %MVN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MVN_URL%' -OutFile '%MVN_ZIP%'"
    if errorlevel 1 (
        echo Failed to download Maven from %MVN_URL%
        exit /b 1
    )
    echo Extracting to %WRAPPER_DIR%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MVN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
    if errorlevel 1 (
        echo Failed to extract Maven
        exit /b 1
    )
    del "%MVN_ZIP%" >nul 2>&1
)

if not defined JAVA_HOME (
    if exist "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
    )
)

call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
