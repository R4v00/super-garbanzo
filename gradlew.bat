@echo off
set DIR=%~dp0
set GRADLEW_JAR=%DIR%\gradle\wrapper\gradle-wrapper.jar

set JAVA_CMD=java
if not "%JAVA_HOME%"=="" set JAVA_CMD=%JAVA_HOME%\bin\java

%JAVA_CMD% %JAVA_OPTS% -jar "%GRADLEW_JAR%" %*
