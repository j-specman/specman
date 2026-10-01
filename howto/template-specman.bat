@echo off
REM Generated from howto/template-specman.bat by 'mvn package' - do not edit target/specman.bat directly

REM Check strategy before starting Specman:
REM   1. Is 'java' on the PATH at all? If not, abort with a download hint.
REM   2. Is its version new enough (>= MIN_JAVA_VERSION)? If not, abort with a download hint.
REM   3. Locate the Specman jar: prefer one next to this script (portable/zip layout, no setup needed),
REM      otherwise fall back to SPECMAN_HOME\<jar> (fileserver install layout).
REM      If neither exists, abort and tell the user both locations that were checked.
REM   4. All checks passed: start Specman.

set "JAR_NAME=specman-@VERSION@-jar-with-dependencies.jar"
set "MIN_JAVA_VERSION=@MIN_JAVA_VERSION@"
set "SCRIPT_DIR=%~dp0"

where java >nul 2>nul
if errorlevel 1 goto :no_java

REM 'java --version' prints several lines on some JDKs (e.g. OpenJDK also prints the runtime and VM
REM lines) - only the first line has the version, so only let the first iteration set the variable,
REM otherwise JAVA_VERSION_FULL would end up holding a fragment of a later, unrelated line.
for /f "tokens=2" %%v in ('java --version 2^>^&1') do if not defined JAVA_VERSION_FULL set "JAVA_VERSION_FULL=%%v"
for /f "delims=." %%m in ("%JAVA_VERSION_FULL%") do set "JAVA_MAJOR=%%m"

if %JAVA_MAJOR% LSS %MIN_JAVA_VERSION% goto :java_too_old

set "JAR_PATH="
if exist "%SCRIPT_DIR%%JAR_NAME%" set "JAR_PATH=%SCRIPT_DIR%%JAR_NAME%"
if not defined JAR_PATH if defined SPECMAN_HOME if exist "%SPECMAN_HOME%\%JAR_NAME%" set "JAR_PATH=%SPECMAN_HOME%\%JAR_NAME%"
if not defined JAR_PATH goto :jar_not_found

javaw -jar "%JAR_PATH%" %*
goto :eof

:no_java
echo Java not found on PATH.
echo Specman requires Java %MIN_JAVA_VERSION% or higher. Download: https://adoptium.net
pause
exit /b 1

:java_too_old
echo Found Java %JAVA_VERSION_FULL%, but Specman requires Java %MIN_JAVA_VERSION% or higher.
echo Download: https://adoptium.net
pause
exit /b 1

:jar_not_found
echo Could not find %JAR_NAME%.
echo Looked in:
echo   %SCRIPT_DIR%
if defined SPECMAN_HOME (echo   %SPECMAN_HOME%) else (echo   ^(SPECMAN_HOME is not set^))
echo Either place the jar next to this script, or set SPECMAN_HOME to the directory containing it.
pause
exit /b 1
