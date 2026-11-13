@echo off
setlocal
cd /d "%~dp0"

if not exist "mssql-jdbc-13.6.2.jre11.jar" (
    echo SQL Server JDBC driver not found.
    echo Place mssql-jdbc-13.6.2.jre11.jar beside this script.
    exit /b 1
)

java -cp "appbuild;.;mssql-jdbc-13.6.2.jre11.jar" com.rfid.LoginPage
endlocal
