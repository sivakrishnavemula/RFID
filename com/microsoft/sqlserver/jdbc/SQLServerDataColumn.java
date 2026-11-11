package com.microsoft.sqlserver.jdbc;

public final class SQLServerDataColumn {
   String columnName;
   int javaSqlType;
   int precision = 0;
   int scale = 0;

   public SQLServerDataColumn(String var1, int var2) {
      this.columnName = var1;
      this.javaSqlType = var2;
   }

   public String getColumnName() {
      return this.columnName;
   }

   public int getColumnType() {
      return this.javaSqlType;
   }
}
