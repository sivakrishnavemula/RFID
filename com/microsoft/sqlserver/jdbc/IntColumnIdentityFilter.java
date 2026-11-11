package com.microsoft.sqlserver.jdbc;

class IntColumnIdentityFilter extends ColumnFilter {
   private static final String zeroOneToYesNo(int var0) {
      return 0 == var0 ? "NO" : "YES";
   }

   final Object apply(Object var1, JDBCType var2) throws SQLServerException {
      if (var1 == null) {
         return var1;
      }

      switch (var2) {
         case INTEGER:
         case SMALLINT:
            assert var1 instanceof Number;
            return zeroOneToYesNo(((Number)var1).intValue());
         case CHAR:
         case VARCHAR:
         case LONGVARCHAR:
            assert var1 instanceof String;
            return zeroOneToYesNo(Integer.parseInt((String)var1));
         default:
            DataTypes.throwConversionError("char", var2.toString());
            return var1;
      }
   }
}
