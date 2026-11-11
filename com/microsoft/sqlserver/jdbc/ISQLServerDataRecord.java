package com.microsoft.sqlserver.jdbc;

public interface ISQLServerDataRecord {
   SQLServerMetaData getColumnMetaData(int var1);

   int getColumnCount();

   Object[] getRowData();

   boolean next();
}
