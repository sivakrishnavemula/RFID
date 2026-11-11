package com.microsoft.sqlserver.jdbc;

abstract class ColumnFilter {
   abstract Object apply(Object var1, JDBCType var2) throws SQLServerException;
}
