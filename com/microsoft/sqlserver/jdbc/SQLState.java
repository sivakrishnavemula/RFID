package com.microsoft.sqlserver.jdbc;

enum SQLState {
   STATEMENT_CANCELED("HY008"),
   DATA_EXCEPTION_NOT_SPECIFIC("22000"),
   DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW("22008"),
   DATA_EXCEPTION_LENGTH_MISMATCH("22026"),
   COL_NOT_FOUND("42S22");

   private final String sqlStateCode;

   final String getSQLStateCode() {
      return this.sqlStateCode;
   }

   SQLState(String var3) {
      this.sqlStateCode = var3;
   }
}
