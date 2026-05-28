@echo off
setlocal

rem TravelMate Windows runner: keep Vietnamese logs readable in VS Code/PowerShell/cmd.
chcp 65001 >nul

set "UTF8_JVM_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
if "%MAVEN_OPTS%"=="" (
    set "MAVEN_OPTS=%UTF8_JVM_OPTS%"
) else (
    set "MAVEN_OPTS=%MAVEN_OPTS% %UTF8_JVM_OPTS%"
)

call "%~dp0mvnw.cmd" "-Dspring-boot.run.jvmArguments=%UTF8_JVM_OPTS%" %* spring-boot:run
