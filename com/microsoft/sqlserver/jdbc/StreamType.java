package com.microsoft.sqlserver.jdbc;

enum StreamType {
   NONE(JDBCType.UNKNOWN, "None"),
   ASCII(JDBCType.LONGVARCHAR, "AsciiStream"),
   BINARY(JDBCType.LONGVARBINARY, "BinaryStream"),
   CHARACTER(JDBCType.LONGVARCHAR, "CharacterStream"),
   NCHARACTER(JDBCType.LONGNVARCHAR, "NCharacterStream"),
   SQLXML(JDBCType.SQLXML, "SQLXML");

   private final JDBCType jdbcType;
   private final String name;

   JDBCType getJDBCType() {
      return this.jdbcType;
   }

   StreamType(JDBCType var3, String var4) {
      this.jdbcType = var3;
      this.name = var4;
   }

   public String toString() {
      return this.name;
   }

   boolean convertsFrom(TypeInfo var1) {
      if (ASCII == this) {
         if (SSType.XML == var1.getSSType()) {
            return false;
         }

         if (null != var1.getSQLCollation() && !var1.getSQLCollation().supportsAsciiConversion()) {
            return false;
         }
      }

      return var1.getSSType().convertsTo(this.jdbcType);
   }

   boolean convertsTo(TypeInfo var1) {
      if (ASCII == this) {
         if (SSType.XML == var1.getSSType()) {
            return false;
         }

         if (null != var1.getSQLCollation() && !var1.getSQLCollation().supportsAsciiConversion()) {
            return false;
         }
      }

      return this.jdbcType.convertsTo(var1.getSSType());
   }
}
