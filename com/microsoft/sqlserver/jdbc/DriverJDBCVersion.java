package com.microsoft.sqlserver.jdbc;

import java.sql.BatchUpdateException;

final class DriverJDBCVersion {
   static final int major = 4;
   static final int minor = 2;

   static final void checkSupportsJDBC4() {
   }

   static final void checkSupportsJDBC41() {
   }

   static final void checkSupportsJDBC42() {
   }

   static final void throwBatchUpdateException(SQLServerException var0, long[] var1) throws BatchUpdateException {
      throw new BatchUpdateException(var0.getMessage(), var0.getSQLState(), var0.getErrorCode(), var1, new Throwable(var0.getMessage()));
   }
}
