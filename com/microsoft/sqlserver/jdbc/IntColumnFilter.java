package com.microsoft.sqlserver.jdbc;

abstract class IntColumnFilter extends ColumnFilter {
   abstract int oneValueToAnother(int var1);

   final Object apply(Object var1, JDBCType var2) throws SQLServerException {
      if (var1 == null) {
         return var1;
      }

      switch (var2) {
         case INTEGER:
            return new Integer(this.oneValueToAnother((Integer)var1));
         case SMALLINT:
         case TINYINT:
            return new Short((short)this.oneValueToAnother(((Short)var1).intValue()));
         case BIGINT:
            return new Long(this.oneValueToAnother(((Long)var1).intValue()));
         case CHAR:
         case VARCHAR:
         case LONGVARCHAR:
            return Integer.toString(this.oneValueToAnother(Integer.parseInt((String)var1)));
         default:
            DataTypes.throwConversionError("int", var2.toString());
            return var1;
      }
   }
}
