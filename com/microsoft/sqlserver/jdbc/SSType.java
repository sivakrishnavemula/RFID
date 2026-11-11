package com.microsoft.sqlserver.jdbc;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.EnumSet;

enum SSType {
   UNKNOWN(SSType.Category.UNKNOWN, "unknown", JDBCType.UNKNOWN),
   TINYINT(SSType.Category.NUMERIC, "tinyint", JDBCType.TINYINT),
   BIT(SSType.Category.NUMERIC, "bit", JDBCType.BIT),
   SMALLINT(SSType.Category.NUMERIC, "smallint", JDBCType.SMALLINT),
   INTEGER(SSType.Category.NUMERIC, "int", JDBCType.INTEGER),
   BIGINT(SSType.Category.NUMERIC, "bigint", JDBCType.BIGINT),
   FLOAT(SSType.Category.NUMERIC, "float", JDBCType.DOUBLE),
   REAL(SSType.Category.NUMERIC, "real", JDBCType.REAL),
   SMALLDATETIME(SSType.Category.DATETIME, "smalldatetime", JDBCType.SMALLDATETIME),
   DATETIME(SSType.Category.DATETIME, "datetime", JDBCType.DATETIME),
   DATE(SSType.Category.DATE, "date", JDBCType.DATE),
   TIME(SSType.Category.TIME, "time", JDBCType.TIME),
   DATETIME2(SSType.Category.DATETIME2, "datetime2", JDBCType.TIMESTAMP),
   DATETIMEOFFSET(SSType.Category.DATETIMEOFFSET, "datetimeoffset", JDBCType.DATETIMEOFFSET),
   SMALLMONEY(SSType.Category.NUMERIC, "smallmoney", JDBCType.SMALLMONEY),
   MONEY(SSType.Category.NUMERIC, "money", JDBCType.MONEY),
   CHAR(SSType.Category.CHARACTER, "char", JDBCType.CHAR),
   VARCHAR(SSType.Category.CHARACTER, "varchar", JDBCType.VARCHAR),
   VARCHARMAX(SSType.Category.LONG_CHARACTER, "varchar", JDBCType.LONGVARCHAR),
   TEXT(SSType.Category.LONG_CHARACTER, "text", JDBCType.LONGVARCHAR),
   NCHAR(SSType.Category.NCHARACTER, "nchar", JDBCType.NCHAR),
   NVARCHAR(SSType.Category.NCHARACTER, "nvarchar", JDBCType.NVARCHAR),
   NVARCHARMAX(SSType.Category.LONG_NCHARACTER, "nvarchar", JDBCType.LONGNVARCHAR),
   NTEXT(SSType.Category.LONG_NCHARACTER, "ntext", JDBCType.LONGNVARCHAR),
   BINARY(SSType.Category.BINARY, "binary", JDBCType.BINARY),
   VARBINARY(SSType.Category.BINARY, "varbinary", JDBCType.VARBINARY),
   VARBINARYMAX(SSType.Category.LONG_BINARY, "varbinary", JDBCType.LONGVARBINARY),
   IMAGE(SSType.Category.LONG_BINARY, "image", JDBCType.LONGVARBINARY),
   DECIMAL(SSType.Category.NUMERIC, "decimal", JDBCType.DECIMAL),
   NUMERIC(SSType.Category.NUMERIC, "numeric", JDBCType.NUMERIC),
   GUID(SSType.Category.GUID, "uniqueidentifier", JDBCType.GUID),
   SQL_VARIANT(SSType.Category.VARIANT, "sql_variant", JDBCType.VARCHAR),
   UDT(SSType.Category.UDT, "udt", JDBCType.VARBINARY),
   XML(SSType.Category.XML, "xml", JDBCType.LONGNVARCHAR),
   TIMESTAMP(SSType.Category.TIMESTAMP, "timestamp", JDBCType.BINARY);

   final SSType.Category category;
   private final String name;
   private final JDBCType jdbcType;
   static final BigDecimal MAX_VALUE_MONEY = new BigDecimal("922337203685477.5807");
   static final BigDecimal MIN_VALUE_MONEY = new BigDecimal("-922337203685477.5808");
   static final BigDecimal MAX_VALUE_SMALLMONEY = new BigDecimal("214748.3647");
   static final BigDecimal MIN_VALUE_SMALLMONEY = new BigDecimal("-214748.3648");

   SSType(SSType.Category var3, String var4, JDBCType var5) {
      this.category = var3;
      this.name = var4;
      this.jdbcType = var5;
   }

   public String toString() {
      return this.name;
   }

   final JDBCType getJDBCType() {
      return this.jdbcType;
   }

   static SSType of(String var0) throws SQLServerException {
      for (SSType var4 : values()) {
         if (var4.name.equalsIgnoreCase(var0)) {
            return var4;
         }
      }

      MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_unknownSSType"));
      Object[] var6 = new Object[]{var0};
      SQLServerException.makeFromDriverError(null, null, var5.format(var6), null, true);
      return UNKNOWN;
   }

   boolean convertsTo(JDBCType var1) {
      return SSType.GetterConversion.converts(this, var1);
   }

   enum Category {
      BINARY,
      CHARACTER,
      DATE,
      DATETIME,
      DATETIME2,
      DATETIMEOFFSET,
      GUID,
      LONG_BINARY,
      LONG_CHARACTER,
      LONG_NCHARACTER,
      NCHARACTER,
      NUMERIC,
      UNKNOWN,
      TIME,
      TIMESTAMP,
      UDT,
      VARIANT,
      XML;
   }

   enum GetterConversion {
      NUMERIC(
         SSType.Category.NUMERIC,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.NUMERIC,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY
         )
      ),
      DATETIME(
         SSType.Category.DATETIME,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY
         )
      ),
      DATETIME2(
         SSType.Category.DATETIME2,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      DATE(
         SSType.Category.DATE,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      TIME(
         SSType.Category.TIME,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      DATETIMEOFFSET(
         SSType.Category.DATETIMEOFFSET,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.DATETIMEOFFSET,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      CHARACTER(
         SSType.Category.CHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.JDBCType.Category[]{
               com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.GUID
            }
         )
      ),
      LONG_CHARACTER(
         SSType.Category.LONG_CHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.JDBCType.Category[]{
               com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CLOB
            }
         )
      ),
      NCHARACTER(
         SSType.Category.NCHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.JDBCType.Category[]{
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP
            }
         )
      ),
      LONG_NCHARACTER(
         SSType.Category.LONG_NCHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.JDBCType.Category[]{
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.DATE,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIME,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.TIMESTAMP,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CLOB,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.NCLOB
            }
         )
      ),
      BINARY(
         SSType.Category.BINARY,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.GUID
         )
      ),
      LONG_BINARY(
         SSType.Category.LONG_BINARY,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BLOB
         )
      ),
      TIMESTAMP(
         SSType.Category.TIMESTAMP,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      XML(
         SSType.Category.XML,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER,
            new com.microsoft.sqlserver.jdbc.JDBCType.Category[]{
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.CLOB,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.NCLOB,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_BINARY,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.BLOB,
               com.microsoft.sqlserver.jdbc.JDBCType.Category.SQLXML
            }
         )
      ),
      UDT(
         SSType.Category.UDT,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.LONG_BINARY,
            com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER
         )
      ),
      GUID(SSType.Category.GUID, EnumSet.of(com.microsoft.sqlserver.jdbc.JDBCType.Category.BINARY, com.microsoft.sqlserver.jdbc.JDBCType.Category.CHARACTER));

      private final SSType.Category from;
      private final EnumSet<com.microsoft.sqlserver.jdbc.JDBCType.Category> to;
      private static final EnumMap<SSType.Category, EnumSet<com.microsoft.sqlserver.jdbc.JDBCType.Category>> conversionMap = new EnumMap(SSType.Category.class);

      GetterConversion(SSType.Category var3, EnumSet<com.microsoft.sqlserver.jdbc.JDBCType.Category> var4) {
         this.from = var3;
         this.to = var4;
      }

      static final boolean converts(SSType var0, JDBCType var1) {
         return ((EnumSet)conversionMap.get(var0.category)).contains(var1.category);
      }

      static {
         for (SSType.Category var3 : SSType.Category.values()) {
            conversionMap.put(var3, EnumSet.noneOf(com.microsoft.sqlserver.jdbc.JDBCType.Category.class));
         }

         for (SSType.GetterConversion var7 : values()) {
            ((EnumSet)conversionMap.get(var7.from)).addAll(var7.to);
         }
      }
   }
}
