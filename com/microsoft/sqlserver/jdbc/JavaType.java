package com.microsoft.sqlserver.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.EnumSet;
import microsoft.sql.DateTimeOffset;

enum JavaType {
   INTEGER(Integer.class, JDBCType.INTEGER),
   STRING(String.class, JDBCType.CHAR),
   DATE(Date.class, JDBCType.DATE),
   TIME(Time.class, JDBCType.TIME),
   TIMESTAMP(Timestamp.class, JDBCType.TIMESTAMP),
   UTILDATE(java.util.Date.class, JDBCType.TIMESTAMP),
   CALENDAR(Calendar.class, JDBCType.TIMESTAMP),
   LOCALDATE(getJavaClass("LocalDate"), JDBCType.DATE),
   LOCALTIME(getJavaClass("LocalTime"), JDBCType.TIME),
   LOCALDATETIME(getJavaClass("LocalDateTime"), JDBCType.TIMESTAMP),
   OFFSETTIME(getJavaClass("OffsetTime"), JDBCType.TIME_WITH_TIMEZONE),
   OFFSETDATETIME(getJavaClass("OffsetDateTime"), JDBCType.TIMESTAMP_WITH_TIMEZONE),
   DATETIMEOFFSET(DateTimeOffset.class, JDBCType.DATETIMEOFFSET),
   BOOLEAN(Boolean.class, JDBCType.BIT),
   BIGDECIMAL(BigDecimal.class, JDBCType.DECIMAL),
   DOUBLE(Double.class, JDBCType.DOUBLE),
   FLOAT(Float.class, JDBCType.REAL),
   SHORT(Short.class, JDBCType.SMALLINT),
   LONG(Long.class, JDBCType.BIGINT),
   BIGINTEGER(BigInteger.class, JDBCType.BIGINT),
   BYTE(Byte.class, JDBCType.TINYINT),
   BYTEARRAY(byte[].class, JDBCType.BINARY),
   NCLOB(NClob.class, JDBCType.NCLOB),
   CLOB(Clob.class, JDBCType.CLOB),
   BLOB(Blob.class, JDBCType.BLOB),
   TVP(TVP.class, JDBCType.TVP),
   INPUTSTREAM(InputStream.class, JDBCType.UNKNOWN) {
      @Override
      JDBCType getJDBCType(SSType var1, JDBCType var2) {
         JDBCType var3;
         if (SSType.UNKNOWN != var1) {
            switch (var1) {
               case CHAR:
               case VARCHAR:
               case VARCHARMAX:
               case TEXT:
               case NCHAR:
               case NVARCHAR:
               case NVARCHARMAX:
               case NTEXT:
                  var3 = JDBCType.LONGVARCHAR;
                  break;
               case XML:
               default:
                  var3 = JDBCType.LONGVARBINARY;
            }
         } else {
            var3 = var2.isTextual() ? JDBCType.LONGVARCHAR : JDBCType.LONGVARBINARY;
         }

         assert null != var3;
         return var3;
      }
   },
   READER(Reader.class, JDBCType.LONGVARCHAR),
   SQLXML(SQLServerSQLXML.class, JDBCType.SQLXML),
   OBJECT(Object.class, JDBCType.UNKNOWN);

   private final Class<?> javaClass;
   private final JDBCType jdbcTypeFromJavaType;
   private static double jvmVersion = 0.0;

   JavaType(Class<?> var3, JDBCType var4) {
      this.javaClass = var3;
      this.jdbcTypeFromJavaType = var4;
   }

   static Class<?> getJavaClass(String var0) {
      if (0.0 == jvmVersion) {
         try {
            String var1 = System.getProperty("java.specification.version");
            if (var1 != null) {
               jvmVersion = Double.parseDouble(var1);
            }
         } catch (NumberFormatException var2) {
            jvmVersion = 0.1;
         }
      }

      if (jvmVersion < 1.8) {
         return TemporalCompatibility.class;
      } else if (var0.equals("LocalDate")) {
         return LocalDate.class;
      } else if (var0.equals("LocalTime")) {
         return LocalTime.class;
      } else if (var0.equals("LocalDateTime")) {
         return LocalDateTime.class;
      } else if (var0.equals("OffsetTime")) {
         return OffsetTime.class;
      } else {
         return var0.equals("OffsetDateTime") ? OffsetDateTime.class : TemporalCompatibility.class;
      }
   }

   static JavaType of(Object var0) {
      if (!(var0 instanceof SQLServerDataTable) && !(var0 instanceof ResultSet) && !(var0 instanceof ISQLServerDataRecord)) {
         if (null != var0) {
            for (JavaType var4 : values()) {
               if (var4.javaClass.isInstance(var0)) {
                  return var4;
               }
            }
         }

         return OBJECT;
      } else {
         return TVP;
      }
   }

   JDBCType getJDBCType(SSType var1, JDBCType var2) {
      return this.jdbcTypeFromJavaType;
   }

   enum SetterConversionAE {
      BIT(JavaType.BOOLEAN, EnumSet.of(JDBCType.BIT, JDBCType.TINYINT, JDBCType.SMALLINT, JDBCType.INTEGER, JDBCType.BIGINT)),
      SHORT(JavaType.SHORT, EnumSet.of(JDBCType.TINYINT, JDBCType.SMALLINT, JDBCType.INTEGER, JDBCType.BIGINT)),
      INTEGER(JavaType.INTEGER, EnumSet.of(JDBCType.INTEGER, JDBCType.BIGINT)),
      LONG(JavaType.LONG, EnumSet.of(JDBCType.BIGINT)),
      BIGDECIMAL(JavaType.BIGDECIMAL, EnumSet.of(JDBCType.MONEY, JDBCType.SMALLMONEY, JDBCType.DECIMAL, JDBCType.NUMERIC)),
      BYTE(JavaType.BYTE, EnumSet.of(JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY, JDBCType.TINYINT)),
      BYTEARRAY(JavaType.BYTEARRAY, EnumSet.of(JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY)),
      DATE(JavaType.DATE, EnumSet.of(JDBCType.DATE)),
      DATETIMEOFFSET(JavaType.DATETIMEOFFSET, EnumSet.of(JDBCType.DATETIMEOFFSET)),
      DOUBLE(JavaType.DOUBLE, EnumSet.of(JDBCType.DOUBLE)),
      FLOAT(JavaType.FLOAT, EnumSet.of(JDBCType.REAL, JDBCType.DOUBLE)),
      STRING(
         JavaType.STRING,
         EnumSet.of(
            JDBCType.CHAR, new JDBCType[]{JDBCType.VARCHAR, JDBCType.LONGVARCHAR, JDBCType.NCHAR, JDBCType.NVARCHAR, JDBCType.LONGNVARCHAR, JDBCType.GUID}
         )
      ),
      TIME(JavaType.TIME, EnumSet.of(JDBCType.TIME)),
      TIMESTAMP(JavaType.TIMESTAMP, EnumSet.of(JDBCType.TIME, JDBCType.TIMESTAMP, JDBCType.DATETIME, JDBCType.SMALLDATETIME));

      private final EnumSet<JDBCType> to;
      private final JavaType from;
      private static final EnumMap<JavaType, EnumSet<JDBCType>> setterConversionAEMap = new EnumMap(JavaType.class);

      SetterConversionAE(JavaType var3, EnumSet<JDBCType> var4) {
         this.from = var3;
         this.to = var4;
      }

      static boolean converts(JavaType var0, JDBCType var1) {
         if (null == var0 || JavaType.OBJECT == var0) {
            return true;
         } else {
            return !setterConversionAEMap.containsKey(var0) ? false : ((EnumSet)setterConversionAEMap.get(var0)).contains(var1);
         }
      }

      static {
         for (JavaType var3 : JavaType.values()) {
            setterConversionAEMap.put(var3, EnumSet.noneOf(JDBCType.class));
         }

         for (JavaType.SetterConversionAE var7 : values()) {
            ((EnumSet)setterConversionAEMap.get(var7.from)).addAll(var7.to);
         }
      }
   }
}
