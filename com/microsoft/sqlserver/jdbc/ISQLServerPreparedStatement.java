package com.microsoft.sqlserver.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import microsoft.sql.DateTimeOffset;

public interface ISQLServerPreparedStatement extends PreparedStatement, ISQLServerStatement {
   void setDateTimeOffset(int var1, DateTimeOffset var2) throws SQLException;
}
