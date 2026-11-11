package com.microsoft.sqlserver.jdbc;

import java.sql.CallableStatement;
import java.sql.SQLException;
import microsoft.sql.DateTimeOffset;

public interface ISQLServerCallableStatement extends CallableStatement, ISQLServerPreparedStatement {
   void setDateTimeOffset(String var1, DateTimeOffset var2) throws SQLException;

   DateTimeOffset getDateTimeOffset(int var1) throws SQLException;

   DateTimeOffset getDateTimeOffset(String var1) throws SQLException;
}
