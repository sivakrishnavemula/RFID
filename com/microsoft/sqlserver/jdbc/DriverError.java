package com.microsoft.sqlserver.jdbc;

enum DriverError {
   NOT_SET(0);

   private final int errorCode;

   final int getErrorCode() {
      return this.errorCode;
   }

   DriverError(int var3) {
      this.errorCode = var3;
   }
}
