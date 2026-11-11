package com.microsoft.sqlserver.jdbc;

public enum SQLServerSortOrder {
   Ascending(0),
   Descending(1),
   Unspecified(-1);

   int value;

   SQLServerSortOrder(int var3) {
      this.value = var3;
   }
}
