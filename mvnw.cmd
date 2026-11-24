@echo off
set "SCRIPT_DIR=%~dp0"
"%SCRIPT_DIR%tools\apache-maven-3.9.9\bin\mvn.cmd" -Dmaven.repo.local="%SCRIPT_DIR%.m2\repository" %*
