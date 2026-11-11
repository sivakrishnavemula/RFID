package com.microsoft.sqlserver.jdbc;

import java.sql.Statement;

public interface ISQLServerStatement extends Statement {
   void setResponseBuffering(String var1) throws SQLServerException;

   String getResponseBuffering() throws SQLServerException;
}
