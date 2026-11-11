package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.EnumSet;

enum JDBCType {
   UNKNOWN(JDBCType.Category.UNKNOWN, 999, "java.lang.Object"),
   ARRAY(JDBCType.Category.UNKNOWN, 2003, "java.lang.Object"),
   BIGINT(JDBCType.Category.NUMERIC, -5, "java.lang.Long"),
   BINARY(JDBCType.Category.BINARY, -2, "[B"),
   BIT(JDBCType.Category.NUMERIC, -7, "java.lang.Boolean"),
   BLOB(JDBCType.Category.BLOB, 2004, "java.sql.Blob"),
   BOOLEAN(JDBCType.Category.NUMERIC, 16, "java.lang.Boolean"),
   CHAR(JDBCType.Category.CHARACTER, 1, "java.lang.String"),
   CLOB(JDBCType.Category.CLOB, 2005, "java.sql.Clob"),
   DATALINK(JDBCType.Category.UNKNOWN, 70, "java.lang.Object"),
   DATE(JDBCType.Category.DATE, 91, "java.sql.Date"),
   DATETIMEOFFSET(JDBCType.Category.DATETIMEOFFSET, -155, "microsoft.sql.DateTimeOffset"),
   DECIMAL(JDBCType.Category.NUMERIC, 3, "java.math.BigDecimal"),
   DISTINCT(JDBCType.Category.UNKNOWN, 2001, "java.lang.Object"),
   DOUBLE(JDBCType.Category.NUMERIC, 8, "java.lang.Double"),
   FLOAT(JDBCType.Category.NUMERIC, 6, "java.lang.Double"),
   INTEGER(JDBCType.Category.NUMERIC, 4, "java.lang.Integer"),
   JAVA_OBJECT(JDBCType.Category.UNKNOWN, 2000, "java.lang.Object"),
   LONGNVARCHAR(JDBCType.Category.LONG_NCHARACTER, -16, "java.lang.String"),
   LONGVARBINARY(JDBCType.Category.LONG_BINARY, -4, "[B"),
   LONGVARCHAR(JDBCType.Category.LONG_CHARACTER, -1, "java.lang.String"),
   NCHAR(JDBCType.Category.NCHARACTER, -15, "java.lang.String"),
   NCLOB(JDBCType.Category.NCLOB, 2011, "java.sql.NClob"),
   NULL(JDBCType.Category.UNKNOWN, 0, "java.lang.Object"),
   NUMERIC(JDBCType.Category.NUMERIC, 2, "java.math.BigDecimal"),
   NVARCHAR(JDBCType.Category.NCHARACTER, -9, "java.lang.String"),
   OTHER(JDBCType.Category.UNKNOWN, 1111, "java.lang.Object"),
   REAL(JDBCType.Category.NUMERIC, 7, "java.lang.Float"),
   REF(JDBCType.Category.UNKNOWN, 2006, "java.lang.Object"),
   ROWID(JDBCType.Category.UNKNOWN, -8, "java.lang.Object"),
   SMALLINT(JDBCType.Category.NUMERIC, 5, "java.lang.Short"),
   SQLXML(JDBCType.Category.SQLXML, 2009, "java.lang.Object"),
   STRUCT(JDBCType.Category.UNKNOWN, 2002, "java.lang.Object"),
   TIME(JDBCType.Category.TIME, 92, "java.sql.Time"),
   TIME_WITH_TIMEZONE(JDBCType.Category.TIME_WITH_TIMEZONE, 2013, "java.time.OffsetTime"),
   TIMESTAMP(JDBCType.Category.TIMESTAMP, 93, "java.sql.Timestamp"),
   TIMESTAMP_WITH_TIMEZONE(JDBCType.Category.TIMESTAMP_WITH_TIMEZONE, 2014, "java.time.OffsetDateTime"),
   TINYINT(JDBCType.Category.NUMERIC, -6, "java.lang.Short"),
   VARBINARY(JDBCType.Category.BINARY, -3, "[B"),
   VARCHAR(JDBCType.Category.CHARACTER, 12, "java.lang.String"),
   MONEY(JDBCType.Category.NUMERIC, -148, "java.math.BigDecimal"),
   SMALLMONEY(JDBCType.Category.NUMERIC, -146, "java.math.BigDecimal"),
   TVP(JDBCType.Category.TVP, -153, "java.lang.Object"),
   DATETIME(JDBCType.Category.TIMESTAMP, -151, "java.sql.Timestamp"),
   SMALLDATETIME(JDBCType.Category.TIMESTAMP, -150, "java.sql.Timestamp"),
   GUID(JDBCType.Category.CHARACTER, -145, "java.lang.String");

   final JDBCType.Category category;
   private final int intValue;
   private final String className;
   private static final EnumSet<JDBCType> signedTypes = EnumSet.of(
      SMALLINT, new JDBCType[]{INTEGER, BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, MONEY, SMALLMONEY}
   );
   private static final EnumSet<JDBCType> binaryTypes = EnumSet.of(BINARY, VARBINARY, LONGVARBINARY, BLOB);
   private static final EnumSet<JDBCType.Category> textualCategories = EnumSet.of(
      JDBCType.Category.CHARACTER,
      new JDBCType.Category[]{
         JDBCType.Category.LONG_CHARACTER, JDBCType.Category.CLOB, JDBCType.Category.NCHARACTER, JDBCType.Category.LONG_NCHARACTER, JDBCType.Category.NCLOB
      }
   );

   final String className() {
      return this.className;
   }

   JDBCType(JDBCType.Category var3, int var4, String var5) {
      this.category = var3;
      this.intValue = var4;
      this.className = var5;
   }

   public int getIntValue() {
      return this.intValue;
   }

   boolean convertsTo(JDBCType var1) {
      return JDBCType.SetterConversion.converts(this, var1);
   }

   boolean convertsTo(SSType var1) {
      return JDBCType.UpdaterConversion.converts(this, var1);
   }

   static JDBCType of(int var0) throws SQLServerException {
      for (JDBCType var4 : values()) {
         if (var4.intValue == var0) {
            return var4;
         }
      }

      MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_unknownJDBCType"));
      Object[] var6 = new Object[]{var0};
      SQLServerException.makeFromDriverError(null, null, var5.format(var6), null, true);
      return UNKNOWN;
   }

   boolean isSigned() {
      return signedTypes.contains(this);
   }

   boolean isBinary() {
      return binaryTypes.contains(this);
   }

   boolean isTextual() {
      return textualCategories.contains(this.category);
   }

   boolean isUnsupported() {
      return JDBCType.Category.UNKNOWN == this.category;
   }

   int asJavaSqlType() {
      if (Util.SYSTEM_SPEC_VERSION.equals("1.5")) {
         switch (this) {
            case NCHAR:
               return 1;
            case NVARCHAR:
               return 12;
            case LONGNVARCHAR:
               return -1;
            case NCLOB:
               return 2005;
            case ROWID:
               return 1111;
            case SQLXML:
               return 12;
            default:
               return this.intValue;
         }
      } else {
         return this.intValue;
      }
   }

   boolean normalizationCheck(SSType var1) {
      return JDBCType.NormalizationAE.converts(this, var1);
   }

   enum Category {
      CHARACTER,
      LONG_CHARACTER,
      CLOB,
      NCHARACTER,
      LONG_NCHARACTER,
      NCLOB,
      BINARY,
      LONG_BINARY,
      BLOB,
      NUMERIC,
      DATE,
      TIME,
      TIMESTAMP,
      TIME_WITH_TIMEZONE,
      TIMESTAMP_WITH_TIMEZONE,
      DATETIMEOFFSET,
      SQLXML,
      UNKNOWN,
      TVP,
      GUID;
   }

   enum NormalizationAE {
      CHARACTER_NORMALIZED_TO(JDBCType.CHAR, EnumSet.of(SSType.CHAR, SSType.VARCHAR, SSType.VARCHARMAX)),
      VARCHARACTER_NORMALIZED_TO(JDBCType.VARCHAR, EnumSet.of(SSType.CHAR, SSType.VARCHAR, SSType.VARCHARMAX)),
      LONGVARCHARACTER_NORMALIZED_TO(JDBCType.LONGVARCHAR, EnumSet.of(SSType.CHAR, SSType.VARCHAR, SSType.VARCHARMAX)),
      NCHAR_NORMALIZED_TO(JDBCType.NCHAR, EnumSet.of(SSType.NCHAR, SSType.NVARCHAR, SSType.NVARCHARMAX)),
      NVARCHAR_NORMALIZED_TO(JDBCType.NVARCHAR, EnumSet.of(SSType.NCHAR, SSType.NVARCHAR, SSType.NVARCHARMAX)),
      LONGNVARCHAR_NORMALIZED_TO(JDBCType.LONGNVARCHAR, EnumSet.of(SSType.NCHAR, SSType.NVARCHAR, SSType.NVARCHARMAX)),
      BIT_NORMALIZED_TO(JDBCType.BIT, EnumSet.of(SSType.BIT, SSType.TINYINT, SSType.SMALLINT, SSType.INTEGER, SSType.BIGINT)),
      TINYINT_NORMALIZED_TO(JDBCType.TINYINT, EnumSet.of(SSType.TINYINT, SSType.SMALLINT, SSType.INTEGER, SSType.BIGINT)),
      SMALLINT_NORMALIZED_TO(JDBCType.SMALLINT, EnumSet.of(SSType.SMALLINT, SSType.INTEGER, SSType.BIGINT)),
      INTEGER_NORMALIZED_TO(JDBCType.INTEGER, EnumSet.of(SSType.INTEGER, SSType.BIGINT)),
      BIGINT_NORMALIZED_TO(JDBCType.BIGINT, EnumSet.of(SSType.BIGINT)),
      BINARY_NORMALIZED_TO(JDBCType.BINARY, EnumSet.of(SSType.BINARY, SSType.VARBINARY, SSType.VARBINARYMAX)),
      VARBINARY_NORMALIZED_TO(JDBCType.VARBINARY, EnumSet.of(SSType.BINARY, SSType.VARBINARY, SSType.VARBINARYMAX)),
      LONGVARBINARY_NORMALIZED_TO(JDBCType.LONGVARBINARY, EnumSet.of(SSType.BINARY, SSType.VARBINARY, SSType.VARBINARYMAX)),
      FLOAT_NORMALIZED_TO(JDBCType.DOUBLE, EnumSet.of(SSType.FLOAT)),
      REAL_NORMALIZED_TO(JDBCType.REAL, EnumSet.of(SSType.REAL)),
      DECIMAL_NORMALIZED_TO(JDBCType.DECIMAL, EnumSet.of(SSType.DECIMAL, SSType.NUMERIC)),
      SMALLMONEY_NORMALIZED_TO(JDBCType.SMALLMONEY, EnumSet.of(SSType.SMALLMONEY, SSType.MONEY)),
      MONEY_NORMALIZED_TO(JDBCType.MONEY, EnumSet.of(SSType.MONEY)),
      NUMERIC_NORMALIZED_TO(JDBCType.NUMERIC, EnumSet.of(SSType.DECIMAL, SSType.NUMERIC)),
      DATE_NORMALIZED_TO(JDBCType.DATE, EnumSet.of(SSType.DATE)),
      TIME_NORMALIZED_TO(JDBCType.TIME, EnumSet.of(SSType.TIME)),
      DATETIME2_NORMALIZED_TO(JDBCType.TIMESTAMP, EnumSet.of(SSType.DATETIME2)),
      DATETIMEOFFSET_NORMALIZED_TO(JDBCType.DATETIMEOFFSET, EnumSet.of(SSType.DATETIMEOFFSET)),
      DATETIME_NORMALIZED_TO(JDBCType.DATETIME, EnumSet.of(SSType.DATETIME)),
      SMALLDATETIME_NORMALIZED_TO(JDBCType.SMALLDATETIME, EnumSet.of(SSType.SMALLDATETIME)),
      GUID_NORMALIZED_TO(JDBCType.GUID, EnumSet.of(SSType.GUID));

      private final JDBCType from;
      private final EnumSet<SSType> to;
      private static final EnumMap<JDBCType, EnumSet<SSType>> normalizationMapAE = new EnumMap(JDBCType.class);

      NormalizationAE(JDBCType var3, EnumSet<SSType> var4) {
         this.from = var3;
         this.to = var4;
      }

      static boolean converts(JDBCType var0, SSType var1) {
         return ((EnumSet)normalizationMapAE.get(var0)).contains(var1);
      }

      static {
         for (JDBCType var3 : JDBCType.values()) {
            normalizationMapAE.put(var3, EnumSet.noneOf(SSType.class));
         }

         for (JDBCType.NormalizationAE var7 : values()) {
            ((EnumSet)normalizationMapAE.get(var7.from)).addAll(var7.to);
         }
      }
   }

   enum SetterConversion {
      CHARACTER(
         JDBCType.Category.CHARACTER,
         EnumSet.of(
            JDBCType.Category.NUMERIC,
            new JDBCType.Category[]{
               JDBCType.Category.DATE,
               JDBCType.Category.TIME,
               JDBCType.Category.TIMESTAMP,
               JDBCType.Category.DATETIMEOFFSET,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER,
               JDBCType.Category.BINARY,
               JDBCType.Category.LONG_BINARY,
               JDBCType.Category.GUID
            }
         )
      ),
      LONG_CHARACTER(
         JDBCType.Category.LONG_CHARACTER,
         EnumSet.of(
            JDBCType.Category.CHARACTER,
            new JDBCType.Category[]{
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER,
               JDBCType.Category.BINARY,
               JDBCType.Category.LONG_BINARY
            }
         )
      ),
      CLOB(JDBCType.Category.CLOB, EnumSet.of(JDBCType.Category.CLOB, JDBCType.Category.LONG_CHARACTER, JDBCType.Category.LONG_NCHARACTER)),
      NCHARACTER(JDBCType.Category.NCHARACTER, EnumSet.of(JDBCType.Category.NCHARACTER, JDBCType.Category.LONG_NCHARACTER, JDBCType.Category.NCLOB)),
      LONG_NCHARACTER(JDBCType.Category.LONG_NCHARACTER, EnumSet.of(JDBCType.Category.NCHARACTER, JDBCType.Category.LONG_NCHARACTER)),
      NCLOB(JDBCType.Category.NCLOB, EnumSet.of(JDBCType.Category.LONG_NCHARACTER, JDBCType.Category.NCLOB)),
      BINARY(
         JDBCType.Category.BINARY,
         EnumSet.of(
            JDBCType.Category.NUMERIC,
            new JDBCType.Category[]{
               JDBCType.Category.DATE,
               JDBCType.Category.TIME,
               JDBCType.Category.TIMESTAMP,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER,
               JDBCType.Category.BINARY,
               JDBCType.Category.LONG_BINARY,
               JDBCType.Category.BLOB,
               JDBCType.Category.GUID
            }
         )
      ),
      LONG_BINARY(JDBCType.Category.LONG_BINARY, EnumSet.of(JDBCType.Category.BINARY, JDBCType.Category.LONG_BINARY)),
      BLOB(JDBCType.Category.BLOB, EnumSet.of(JDBCType.Category.LONG_BINARY, JDBCType.Category.BLOB)),
      NUMERIC(
         JDBCType.Category.NUMERIC,
         EnumSet.of(
            JDBCType.Category.NUMERIC,
            JDBCType.Category.CHARACTER,
            JDBCType.Category.LONG_CHARACTER,
            JDBCType.Category.NCHARACTER,
            JDBCType.Category.LONG_NCHARACTER
         )
      ),
      DATE(
         JDBCType.Category.DATE,
         EnumSet.of(
            JDBCType.Category.DATE,
            new JDBCType.Category[]{
               JDBCType.Category.TIMESTAMP,
               JDBCType.Category.DATETIMEOFFSET,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIME(
         JDBCType.Category.TIME,
         EnumSet.of(
            JDBCType.Category.TIME,
            new JDBCType.Category[]{
               JDBCType.Category.TIMESTAMP,
               JDBCType.Category.DATETIMEOFFSET,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIMESTAMP(
         JDBCType.Category.TIMESTAMP,
         EnumSet.of(
            JDBCType.Category.DATE,
            new JDBCType.Category[]{
               JDBCType.Category.TIME,
               JDBCType.Category.TIMESTAMP,
               JDBCType.Category.DATETIMEOFFSET,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIME_WITH_TIMEZONE(
         JDBCType.Category.TIME_WITH_TIMEZONE,
         EnumSet.of(
            JDBCType.Category.TIME_WITH_TIMEZONE,
            JDBCType.Category.CHARACTER,
            JDBCType.Category.LONG_CHARACTER,
            JDBCType.Category.NCHARACTER,
            JDBCType.Category.LONG_NCHARACTER
         )
      ),
      TIMESTAMP_WITH_TIMEZONE(
         JDBCType.Category.TIMESTAMP_WITH_TIMEZONE,
         EnumSet.of(
            JDBCType.Category.TIMESTAMP_WITH_TIMEZONE,
            new JDBCType.Category[]{
               JDBCType.Category.TIME_WITH_TIMEZONE,
               JDBCType.Category.CHARACTER,
               JDBCType.Category.LONG_CHARACTER,
               JDBCType.Category.NCHARACTER,
               JDBCType.Category.LONG_NCHARACTER
            }
         )
      ),
      DATETIMEOFFSET(
         JDBCType.Category.DATETIMEOFFSET,
         EnumSet.of(JDBCType.Category.DATE, JDBCType.Category.TIME, JDBCType.Category.TIMESTAMP, JDBCType.Category.DATETIMEOFFSET)
      ),
      SQLXML(JDBCType.Category.SQLXML, EnumSet.of(JDBCType.Category.SQLXML)),
      TVP(JDBCType.Category.TVP, EnumSet.of(JDBCType.Category.TVP));

      private final JDBCType.Category from;
      private final EnumSet<JDBCType.Category> to;
      private static final EnumMap<JDBCType.Category, EnumSet<JDBCType.Category>> conversionMap = new EnumMap(JDBCType.Category.class);

      SetterConversion(JDBCType.Category var3, EnumSet<JDBCType.Category> var4) {
         this.from = var3;
         this.to = var4;
      }

      static boolean converts(JDBCType var0, JDBCType var1) {
         return ((EnumSet)conversionMap.get(var0.category)).contains(var1.category);
      }

      static {
         for (JDBCType.Category var3 : JDBCType.Category.values()) {
            conversionMap.put(var3, EnumSet.noneOf(JDBCType.Category.class));
         }

         for (JDBCType.SetterConversion var7 : values()) {
            ((EnumSet)conversionMap.get(var7.from)).addAll(var7.to);
         }
      }
   }

   enum UpdaterConversion {
      CHARACTER(
         JDBCType.Category.CHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.DATE,
               com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.XML,
               com.microsoft.sqlserver.jdbc.SSType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_BINARY,
               com.microsoft.sqlserver.jdbc.SSType.Category.UDT,
               com.microsoft.sqlserver.jdbc.SSType.Category.GUID,
               com.microsoft.sqlserver.jdbc.SSType.Category.TIMESTAMP
            }
         )
      ),
      LONG_CHARACTER(
         JDBCType.Category.LONG_CHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.XML,
               com.microsoft.sqlserver.jdbc.SSType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_BINARY
            }
         )
      ),
      CLOB(
         JDBCType.Category.CLOB,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.XML
         )
      ),
      NCHARACTER(
         JDBCType.Category.NCHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.XML
         )
      ),
      LONG_NCHARACTER(
         JDBCType.Category.LONG_NCHARACTER,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.XML
         )
      ),
      NCLOB(JDBCType.Category.NCLOB, EnumSet.of(com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER, com.microsoft.sqlserver.jdbc.SSType.Category.XML)),
      BINARY(
         JDBCType.Category.BINARY,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.NUMERIC,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.XML,
               com.microsoft.sqlserver.jdbc.SSType.Category.BINARY,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_BINARY,
               com.microsoft.sqlserver.jdbc.SSType.Category.UDT,
               com.microsoft.sqlserver.jdbc.SSType.Category.TIMESTAMP,
               com.microsoft.sqlserver.jdbc.SSType.Category.GUID
            }
         )
      ),
      LONG_BINARY(
         JDBCType.Category.LONG_BINARY,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.XML,
            com.microsoft.sqlserver.jdbc.SSType.Category.BINARY,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_BINARY,
            com.microsoft.sqlserver.jdbc.SSType.Category.UDT
         )
      ),
      BLOB(JDBCType.Category.BLOB, EnumSet.of(com.microsoft.sqlserver.jdbc.SSType.Category.LONG_BINARY, com.microsoft.sqlserver.jdbc.SSType.Category.XML)),
      SQLXML(JDBCType.Category.SQLXML, EnumSet.of(com.microsoft.sqlserver.jdbc.SSType.Category.XML)),
      NUMERIC(
         JDBCType.Category.NUMERIC,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.NUMERIC,
            com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
            com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
         )
      ),
      DATE(
         JDBCType.Category.DATE,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.DATE,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIME(
         JDBCType.Category.TIME,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIMESTAMP(
         JDBCType.Category.TIMESTAMP,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.DATE,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      ),
      DATETIMEOFFSET(
         JDBCType.Category.DATETIMEOFFSET,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.DATE,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIME_WITH_TIMEZONE(
         JDBCType.Category.TIME_WITH_TIMEZONE,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      ),
      TIMESTAMP_WITH_TIMEZONE(
         JDBCType.Category.TIMESTAMP_WITH_TIMEZONE,
         EnumSet.of(
            com.microsoft.sqlserver.jdbc.SSType.Category.DATE,
            new com.microsoft.sqlserver.jdbc.SSType.Category[]{
               com.microsoft.sqlserver.jdbc.SSType.Category.TIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIME2,
               com.microsoft.sqlserver.jdbc.SSType.Category.DATETIMEOFFSET,
               com.microsoft.sqlserver.jdbc.SSType.Category.CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_CHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.NCHARACTER,
               com.microsoft.sqlserver.jdbc.SSType.Category.LONG_NCHARACTER
            }
         )
      );

      private final JDBCType.Category from;
      private final EnumSet<com.microsoft.sqlserver.jdbc.SSType.Category> to;
      private static final EnumMap<JDBCType.Category, EnumSet<com.microsoft.sqlserver.jdbc.SSType.Category>> conversionMap = new EnumMap(
         JDBCType.Category.class
      );

      UpdaterConversion(JDBCType.Category var3, EnumSet<com.microsoft.sqlserver.jdbc.SSType.Category> var4) {
         this.from = var3;
         this.to = var4;
      }

      static boolean converts(JDBCType var0, SSType var1) {
         return ((EnumSet)conversionMap.get(var0.category)).contains(var1.category);
      }

      static {
         for (JDBCType.Category var3 : JDBCType.Category.values()) {
            conversionMap.put(var3, EnumSet.noneOf(com.microsoft.sqlserver.jdbc.SSType.Category.class));
         }

         for (JDBCType.UpdaterConversion var7 : values()) {
            ((EnumSet)conversionMap.get(var7.from)).addAll(var7.to);
         }
      }
   }
}
