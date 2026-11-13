@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "JAVA_HOME=C:\Users\delixfreak\.jdks\corretto-21.0.10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Project folder: %cd%
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: java.exe not found here:
    echo %JAVA_HOME%\bin\java.exe
    pause
    exit /b 1
)

if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    echo Run this command first:
    echo gradle wrapper --gradle-version 9.2.1
    pause
    exit /b 1
)

if not exist "release" (
    mkdir "release"
)

echo Cleaning release folder...
del /Q "release\*.jar" 2>nul

set VERSIONS=1.21.1 1.21.2 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11

for %%v in (%VERSIONS%) do (
    echo.
    echo ================================
    echo Resolving versions for %%v
    echo ================================

    set "YARN="
    set "LOADER="
    set "FABRIC_API="
    set "BUILT_JAR="

    for /f "usebackq delims=" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$r=Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/yarn/%%v'; $r[0].version"`) do (
        set "YARN=%%a"
    )

    for /f "usebackq delims=" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$r=Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/loader'; $r[0].version"`) do (
        set "LOADER=%%a"
    )

    for /f "usebackq delims=" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$mc='%%v'; $url='https://api.modrinth.com/v2/project/P7dR8mSH/version?loaders=[\"\"fabric\"\"]'; $r=Invoke-RestMethod $url; $v=$r | Where-Object { ($_.game_versions -contains $mc) -and ($_.version_number -like ('*+' + $mc)) } | Sort-Object date_published -Descending | Select-Object -First 1; if ($null -eq $v) { exit 1 } else { $v.version_number }"`) do (
        set "FABRIC_API=%%a"
    )

    if "!YARN!"=="" (
        echo ERROR: Failed to get Yarn mappings for %%v
        pause
        exit /b 1
    )

    if "!LOADER!"=="" (
        echo ERROR: Failed to get Fabric Loader version
        pause
        exit /b 1
    )

    if "!FABRIC_API!"=="" (
        echo ERROR: Failed to get Fabric API version for %%v
        pause
        exit /b 1
    )

    echo Minecraft: %%v
    echo Yarn: !YARN!
    echo Loader: !LOADER!
    echo Fabric API: !FABRIC_API!

    echo.
    echo ================================
    echo Building FunEnchantments for %%v
    echo ================================

    if exist "build\libs" (
        del /Q "build\libs\*.jar" 2>nul
    )

    call "%~dp0gradlew.bat" clean build ^
        -Pminecraft_version=%%v ^
        -Pyarn_mappings=!YARN! ^
        -Ploader_version=!LOADER! ^
        -Pfabric_version=!FABRIC_API! ^
        -Pmod_version=1.0.0-%%v

    if errorlevel 1 (
        echo.
        echo ERROR: Build failed for %%v
        pause
        exit /b 1
    )

    echo.
    echo Searching built jar...
    dir /B "build\libs\*.jar"

    for /f "delims=" %%f in ('dir /B /A-D "build\libs\*.jar" ^| findstr /I /V "sources" ^| findstr /I /V "dev"') do (
        set "BUILT_JAR=build\libs\%%f"
    )

    if "!BUILT_JAR!"=="" (
        echo ERROR: Could not find release jar for %%v
        echo Files in build\libs:
        dir "build\libs"
        pause
        exit /b 1
    )

    echo Found release jar: !BUILT_JAR!
    copy /Y "!BUILT_JAR!" "release\funenchantments-%%v.jar"

    if errorlevel 1 (
        echo ERROR: Failed to copy jar to release folder
        pause
        exit /b 1
    )

    echo Copied to release\funenchantments-%%v.jar
)

echo.
echo ================================
echo Done!
echo Files are in:
echo %cd%\release
echo ================================
dir "release"
pause