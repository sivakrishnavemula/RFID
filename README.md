# RFID

## Run on Windows

Make sure `rfid.properties` contains the SQL Server connection settings, then run
`run-rfid.bat`. It starts the application with the Microsoft SQL Server JDBC
driver (`mssql-jdbc-13.6.2.jre11.jar`) on the classpath.

To run the packaged application directly, keep `RFID-Read-Write.jar`,
`mssql-jdbc-13.6.2.jre11.jar`, and `rfid.properties` in the same folder and run:

```powershell
java -jar RFID-Read-Write.jar
```
