package com.microsoft.sqlserver.jdbc;

class ZeroFixupFilter extends IntColumnFilter {
   int oneValueToAnother(int var1) {
      return 0 == var1 ? Integer.MAX_VALUE : var1;
   }
}
