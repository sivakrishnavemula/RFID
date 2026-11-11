package com.microsoft.sqlserver.jdbc;

import com.microsoft.sqlserver.jdbc.JavaType.SetterConversionAE;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import microsoft.sql.DateTimeOffset;

final class DTV {
   private static final Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.DTV");
   private DTVImpl impl;
   CryptoMetadata cryptoMeta = null;
   JDBCType jdbcTypeSetByUser = null;
   int valueLength = 0;

   void setValue(
      SQLCollation var1, JDBCType var2, Object var3, JavaType var4, StreamSetterArgs var5, Calendar var6, Integer var7, SQLServerConnection var8, boolean var9
   ) throws SQLServerException {
      if (null == this.impl) {
         this.impl = new AppDTVImpl();
      }

      this.impl.setValue(this, var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   final void setValue(Object var1, JavaType var2) {
      this.impl.setValue(var1, var2);
   }

   final void clear() {
      this.impl = null;
   }

   final void skipValue(TypeInfo var1, TDSReader var2, boolean var3) throws SQLServerException {
      if (null == this.impl) {
         this.impl = new ServerDTVImpl();
      }

      this.impl.skipValue(var1, var2, var3);
   }

   final void initFromCompressedNull() {
      if (null == this.impl) {
         this.impl = new ServerDTVImpl();
      }

      this.impl.initFromCompressedNull();
   }

   final void setStreamSetterArgs(StreamSetterArgs var1) {
      this.impl.setStreamSetterArgs(var1);
   }

   final void setCalendar(Calendar var1) {
      this.impl.setCalendar(var1);
   }

   final void setScale(Integer var1) {
      this.impl.setScale(var1);
   }

   final void setForceEncrypt(boolean var1) {
      this.impl.setForceEncrypt(var1);
   }

   StreamSetterArgs getStreamSetterArgs() {
      return this.impl.getStreamSetterArgs();
   }

   Calendar getCalendar() {
      return this.impl.getCalendar();
   }

   Integer getScale() {
      return this.impl.getScale();
   }

   boolean isNull() {
      return null == this.impl || this.impl.isNull();
   }

   final boolean isInitialized() {
      return null != this.impl;
   }

   final void setJdbcType(JDBCType var1) {
      if (null == this.impl) {
         this.impl = new AppDTVImpl();
      }

      this.impl.setJdbcType(var1);
   }

   final JDBCType getJdbcType() {
      assert null != this.impl;
      return this.impl.getJdbcType();
   }

   final JavaType getJavaType() {
      assert null != this.impl;
      return this.impl.getJavaType();
   }

   Object getValue(JDBCType var1, int var2, InputStreamGetterArgs var3, Calendar var4, TypeInfo var5, CryptoMetadata var6, TDSReader var7) throws SQLServerException {
      if (null == this.impl) {
         this.impl = new ServerDTVImpl();
      }

      return this.impl.getValue(this, var1, var2, var3, var4, var5, var6, var7);
   }

   Object getSetterValue() {
      return this.impl.getSetterValue();
   }

   void setImpl(DTVImpl var1) {
      this.impl = var1;
   }

   final void executeOp(DTVExecuteOp var1) throws SQLServerException {
      JDBCType var2 = this.getJdbcType();
      Object var3 = this.getSetterValue();
      JavaType var4 = this.getJavaType();
      boolean var5 = false;
      byte[] var6 = null;
      if (null != this.cryptoMeta && !SetterConversionAE.converts(var4, var2)) {
         MessageFormat var29 = new MessageFormat(SQLServerException.getErrString("R_unsupportedConversionAE"));
         Object[] var36 = new Object[]{var4.toString().toLowerCase(Locale.ENGLISH), var2.toString().toLowerCase(Locale.ENGLISH)};
         throw new SQLServerException(var29.format(var36), null);
      }

      if (null == var3) {
         switch (var2) {
            case DATETIME:
            case SMALLDATETIME:
            case TIMESTAMP:
               var1.execute(this, (Timestamp)null);
               break;
            case TIME:
               var1.execute(this, (Time)null);
               break;
            case DATE:
               var1.execute(this, (Date)null);
               break;
            case TIMESTAMP_WITH_TIMEZONE:
            case DATETIMEOFFSET:
            case TIME_WITH_TIMEZONE:
               var1.execute(this, (DateTimeOffset)null);
               break;
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            case NCLOB:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (String)null);
               }
               break;
            case INTEGER:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Integer)null);
               }
               break;
            case FLOAT:
            case REAL:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Float)null);
               }
               break;
            case NUMERIC:
            case DECIMAL:
            case MONEY:
            case SMALLMONEY:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (BigDecimal)null);
               }
               break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case CLOB:
            case GUID:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (byte[])null);
               }
               break;
            case TINYINT:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Byte)null);
               }
               break;
            case BIGINT:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Long)null);
               }
               break;
            case DOUBLE:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Double)null);
               }
               break;
            case SMALLINT:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Short)null);
               }
               break;
            case BIT:
            case BOOLEAN:
               if (null != this.cryptoMeta) {
                  var1.execute(this, (byte[])null);
               } else {
                  var1.execute(this, (Boolean)null);
               }
               break;
            case SQLXML:
               var1.execute(this, (SQLServerSQLXML)null);
               break;
            case ARRAY:
            case DATALINK:
            case DISTINCT:
            case JAVA_OBJECT:
            case NULL:
            case OTHER:
            case REF:
            case ROWID:
            case STRUCT:
               var5 = true;
               break;
            case UNKNOWN:
            default:
               assert false : "Unexpected JDBCType: " + var2;
               var5 = true;
         }
      } else {
         if (aeLogger.isLoggable(Level.FINE) && null != this.cryptoMeta) {
            aeLogger.fine("Encrypting java data type: " + var4);
         }

         switch (var4) {
            case TIME:
               var1.execute(this, (Time)var3);
               break;
            case DATE:
               var1.execute(this, (Date)var3);
               break;
            case TIMESTAMP:
               if (JDBCType.SMALLDATETIME == var2) {
                  Timestamp var27 = (Timestamp)var3;
                  if (var27.after(TDS.MAX_TIMESTAMP) || var27.before(TDS.MIN_TIMESTAMP)) {
                     MessageFormat var34 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
                     throw new SQLServerException(var34.format(new Object[]{var2}), null, 0, null);
                  }
               }

               var1.execute(this, (Timestamp)var3);
               break;
            case UTILDATE:
               var1.execute(this, (java.util.Date)var3);
               break;
            case CALENDAR:
               var1.execute(this, (Calendar)var3);
               break;
            case LOCALDATE:
               var1.execute(this, (LocalDate)var3);
               break;
            case LOCALTIME:
               var1.execute(this, (LocalTime)var3);
               break;
            case LOCALDATETIME:
               var1.execute(this, (LocalDateTime)var3);
               break;
            case OFFSETTIME:
               var1.execute(this, (OffsetTime)var3);
               break;
            case OFFSETDATETIME:
               var1.execute(this, (OffsetDateTime)var3);
               break;
            case DATETIMEOFFSET:
               var1.execute(this, (DateTimeOffset)var3);
               break;
            case STRING:
               if (JDBCType.GUID == var2) {
                  if (var3 instanceof String) {
                     var3 = UUID.fromString((String)var3);
                  }

                  byte[] var24 = Util.asGuidByteArray((UUID)var3);
                  var1.execute(this, var24);
               } else if (null != this.cryptoMeta) {
                  if (var2 == JDBCType.LONGNVARCHAR && JDBCType.VARCHAR == this.jdbcTypeSetByUser && Integer.MAX_VALUE < this.valueLength) {
                     MessageFormat var26 = new MessageFormat(SQLServerException.getErrString("R_StreamingDataTypeAE"));
                     Object[] var33 = new Object[]{Integer.MAX_VALUE, JDBCType.LONGVARCHAR};
                     throw new SQLServerException(this, var26.format(var33), null, 0, false);
                  }

                  if (JDBCType.NVARCHAR == this.jdbcTypeSetByUser && 1073741823 < this.valueLength) {
                     MessageFormat var25 = new MessageFormat(SQLServerException.getErrString("R_StreamingDataTypeAE"));
                     Object[] var32 = new Object[]{1073741823, JDBCType.LONGNVARCHAR};
                     throw new SQLServerException(this, var25.format(var32), null, 0, false);
                  }

                  if (JDBCType.NVARCHAR == this.jdbcTypeSetByUser
                     || JDBCType.NCHAR == this.jdbcTypeSetByUser
                     || JDBCType.LONGNVARCHAR == this.jdbcTypeSetByUser) {
                     var6 = ((String)var3).getBytes(Charset.forName("UTF-16LE"));
                  } else if (JDBCType.VARCHAR == this.jdbcTypeSetByUser
                     || JDBCType.CHAR == this.jdbcTypeSetByUser
                     || JDBCType.LONGVARCHAR == this.jdbcTypeSetByUser) {
                     var6 = ((String)var3).getBytes();
                  }

                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (String)var3);
               }
               break;
            case INTEGER:
               if (null != this.cryptoMeta) {
                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(((Integer)var3).longValue()).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Integer)var3);
               }
               break;
            case TVP:
               var1.execute(this, (TVP)var3);
               break;
            case FLOAT:
               if (null != this.cryptoMeta) {
                  if (Float.isInfinite((Float)var3)) {
                     MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
                     throw new SQLServerException(var23.format(new Object[]{var2}), null, 0, null);
                  }

                  var6 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat((Float)var3).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Float)var3);
               }
               break;
            case BIGDECIMAL:
               if (null != this.cryptoMeta) {
                  if (JDBCType.MONEY != var2 && JDBCType.SMALLMONEY != var2) {
                     BigDecimal var22 = (BigDecimal)var3;
                     byte[] var31 = DDC.convertBigDecimalToBytes(var22, var22.scale());
                     var6 = new byte[16];
                     System.arraycopy(var31, 2, var6, 0, var31.length - 2);
                     this.setScale(var22.scale());
                     var1.execute(this, var6);
                  } else {
                     BigDecimal var21 = (BigDecimal)var3;
                     Util.validateMoneyRange(var21, var2);
                     int var30 = Math.max(var21.precision() - var21.scale(), 0) + 4;
                     long var9 = ((BigDecimal)var3).multiply(new BigDecimal(10000), new MathContext(var30, RoundingMode.HALF_UP)).longValue();
                     ByteBuffer var11 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
                     var11.putInt((int)(var9 >> 32)).array();
                     var11.putInt((int)var9).array();
                     var1.execute(this, var11.array());
                  }
               } else {
                  var1.execute(this, (BigDecimal)var3);
               }
               break;
            case BYTEARRAY:
               if (null != this.cryptoMeta && Integer.MAX_VALUE < this.valueLength) {
                  MessageFormat var20 = new MessageFormat(SQLServerException.getErrString("R_StreamingDataTypeAE"));
                  Object[] var8 = new Object[]{Integer.MAX_VALUE, JDBCType.BINARY};
                  throw new SQLServerException(this, var20.format(var8), null, 0, false);
               }

               var1.execute(this, (byte[])var3);
               break;
            case BYTE:
               if (null != this.cryptoMeta) {
                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong((Byte)var3 & 0xFF).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Byte)var3);
               }
               break;
            case LONG:
               if (null != this.cryptoMeta) {
                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong((Long)var3).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Long)var3);
               }
               break;
            case BIGINTEGER:
               var1.execute(this, (BigInteger)var3);
               break;
            case DOUBLE:
               if (null != this.cryptoMeta) {
                  if (Double.isInfinite((Double)var3)) {
                     MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
                     throw new SQLServerException(var7.format(new Object[]{var2}), null, 0, null);
                  }

                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble((Double)var3).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Double)var3);
               }
               break;
            case SHORT:
               if (null != this.cryptoMeta) {
                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(((Short)var3).shortValue()).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Short)var3);
               }
               break;
            case BOOLEAN:
               if (null != this.cryptoMeta) {
                  var6 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong((Boolean)var3 ? 1L : 0L).array();
                  var1.execute(this, var6);
               } else {
                  var1.execute(this, (Boolean)var3);
               }
               break;
            case BLOB:
               var1.execute(this, (Blob)var3);
               break;
            case CLOB:
            case NCLOB:
               var1.execute(this, (Clob)var3);
               break;
            case INPUTSTREAM:
               var1.execute(this, (InputStream)var3);
               break;
            case READER:
               var1.execute(this, (Reader)var3);
               break;
            case SQLXML:
               var1.execute(this, (SQLServerSQLXML)var3);
               break;
            default:
               assert false : "Unexpected JavaType: " + var4;
               var5 = true;
         }
      }

      if (var5) {
         MessageFormat var28 = new MessageFormat(SQLServerException.getErrString("R_unsupportedConversionFromTo"));
         Object[] var35 = new Object[]{var4, var2};
         throw new SQLServerException(var28.format(var35), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }
   }

   void sendCryptoMetaData(CryptoMetadata var1, TDSWriter var2) {
      this.cryptoMeta = var1;
      var2.setCryptoMetaData(var1);
   }

   void jdbcTypeSetByUser(JDBCType var1, int var2) {
      this.jdbcTypeSetByUser = var1;
      this.valueLength = var2;
   }

   void sendByRPC(String var1, TypeInfo var2, SQLCollation var3, int var4, int var5, boolean var6, TDSWriter var7, SQLServerConnection var8) throws SQLServerException {
      this.executeOp(new DTV.SendByRPCOp(var1, var2, var3, var4, var5, var6, var7, var8));
   }

   final class SendByRPCOp extends DTVExecuteOp {
      private final String name;
      private final TypeInfo typeInfo;
      private final SQLCollation collation;
      private final int precision;
      private final int outScale;
      private final boolean isOutParam;
      private final TDSWriter tdsWriter;
      private final SQLServerConnection conn;

      SendByRPCOp(String var2, TypeInfo var3, SQLCollation var4, int var5, int var6, boolean var7, TDSWriter var8, SQLServerConnection var9) {
         this.name = var2;
         this.typeInfo = var3;
         this.collation = var4;
         this.precision = var5;
         this.outScale = var6;
         this.isOutParam = var7;
         this.tdsWriter = var8;
         this.conn = var9;
      }

      void execute(DTV var1, String var2) throws SQLServerException {
         this.tdsWriter.writeRPCStringUnicode(this.name, var2, this.isOutParam, this.collation);
      }

      void execute(DTV var1, Clob var2) throws SQLServerException {
         assert null != var2;
         long var3 = 0L;
         Reader var5 = null;

         try {
            var3 = DataTypes.getCheckedLength(this.conn, var1.getJdbcType(), var2.length(), false);
            var5 = var2.getCharacterStream();
         } catch (SQLException var12) {
            SQLServerException.makeFromDriverError(this.conn, null, var12.getMessage(), null, false);
         }

         JDBCType var6 = var1.getJdbcType();
         if (null != this.collation && (JDBCType.CHAR == var6 || JDBCType.VARCHAR == var6 || JDBCType.LONGVARCHAR == var6 || JDBCType.CLOB == var6)) {
            if (null == var5) {
               this.tdsWriter.writeRPCByteArray(this.name, null, this.isOutParam, var6, this.collation);
            } else {
               ReaderInputStream var7 = null;

               try {
                  var7 = new ReaderInputStream(var5, this.collation.getCharset(), var3);
               } catch (UnsupportedEncodingException var11) {
                  MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_encodingErrorWritingTDS"));
                  Object[] var10 = new Object[]{new String(var11.getMessage())};
                  SQLServerException.makeFromDriverError(this.conn, null, var9.format(var10), null, true);
               }

               this.tdsWriter.writeRPCInputStream(this.name, var7, -1L, this.isOutParam, var6, this.collation);
            }
         } else if (null == var5) {
            this.tdsWriter.writeRPCStringUnicode(this.name, null, this.isOutParam, this.collation);
         } else {
            this.tdsWriter.writeRPCReaderUnicode(this.name, var5, var3, this.isOutParam, this.collation);
         }
      }

      void execute(DTV var1, Byte var2) throws SQLServerException {
         this.tdsWriter.writeRPCByte(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, Integer var2) throws SQLServerException {
         this.tdsWriter.writeRPCInt(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, Time var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.TIME, var2);
      }

      void execute(DTV var1, Date var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.DATE, var2);
      }

      void execute(DTV var1, Timestamp var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.TIMESTAMP, var2);
      }

      void execute(DTV var1, java.util.Date var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.UTILDATE, var2);
      }

      void execute(DTV var1, Calendar var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.CALENDAR, var2);
      }

      void execute(DTV var1, LocalDate var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.LOCALDATE, var2);
      }

      void execute(DTV var1, LocalTime var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.LOCALTIME, var2);
      }

      void execute(DTV var1, LocalDateTime var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.LOCALDATETIME, var2);
      }

      void execute(DTV var1, OffsetTime var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.OFFSETTIME, var2);
      }

      void execute(DTV var1, OffsetDateTime var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.OFFSETDATETIME, var2);
      }

      void execute(DTV var1, DateTimeOffset var2) throws SQLServerException {
         this.sendTemporal(var1, JavaType.DATETIMEOFFSET, var2);
      }

      void execute(DTV var1, TVP var2) throws SQLServerException {
         this.tdsWriter.writeTVP(var2);
      }

      private void clearSetCalendar(Calendar var1, boolean var2, Integer var3, Integer var4, Integer var5, Integer var6, Integer var7, Integer var8) {
         var1.clear();
         var1.setLenient(var2);
         if (null != var3) {
            var1.set(1, var3);
         }

         if (null != var4) {
            var1.set(2, var4);
         }

         if (null != var5) {
            var1.set(5, var5);
         }

         if (null != var6) {
            var1.set(11, var6);
         }

         if (null != var7) {
            var1.set(12, var7);
         }

         if (null != var8) {
            var1.set(13, var8);
         }
      }

      private void sendTemporal(DTV var1, JavaType var2, Object var3) throws SQLServerException {
         JDBCType var4 = var1.getJdbcType();
         GregorianCalendar var5 = null;
         int var6 = 0;
         int var7 = 0;
         if (null != var3) {
            Object var8 = TimeZone.getDefault();
            long var9 = 0L;
            switch (var2) {
               case TIME:
                  var8 = null != var1.getCalendar() ? var1.getCalendar().getTimeZone() : TimeZone.getDefault();
                  var9 = ((Time)var3).getTime();
                  var6 = 1000000 * (int)(var9 % 1000L);
                  if (var6 < 0) {
                     var6 += 1000000000;
                  }
                  break;
               case DATE:
                  var8 = null != var1.getCalendar() ? var1.getCalendar().getTimeZone() : TimeZone.getDefault();
                  var9 = ((Date)var3).getTime();
                  break;
               case TIMESTAMP:
                  var8 = null != var1.getCalendar() ? var1.getCalendar().getTimeZone() : TimeZone.getDefault();
                  Timestamp var20 = (Timestamp)var3;
                  var9 = var20.getTime();
                  var6 = var20.getNanos();
                  break;
               case UTILDATE:
                  var8 = null != var1.getCalendar() ? var1.getCalendar().getTimeZone() : TimeZone.getDefault();
                  var9 = ((java.util.Date)var3).getTime();
                  var6 = 1000000 * (int)(var9 % 1000L);
                  if (var6 < 0) {
                     var6 += 1000000000;
                  }
                  break;
               case CALENDAR:
                  var8 = null != var1.getCalendar() ? var1.getCalendar().getTimeZone() : TimeZone.getDefault();
                  var9 = ((Calendar)var3).getTimeInMillis();
                  var6 = 1000000 * (int)(var9 % 1000L);
                  if (var6 < 0) {
                     var6 += 1000000000;
                  }
                  break;
               case LOCALDATE:
                  var5 = new GregorianCalendar(UTC.timeZone, Locale.US);
                  this.clearSetCalendar(
                     var5, true, ((LocalDate)var3).getYear(), ((LocalDate)var3).getMonthValue() - 1, ((LocalDate)var3).getDayOfMonth(), null, null, null
                  );
                  break;
               case LOCALTIME:
                  var5 = new GregorianCalendar(UTC.timeZone, Locale.US);
                  LocalTime var11 = (LocalTime)var3;
                  this.clearSetCalendar(var5, true, this.conn.baseYear(), 1, 1, var11.getHour(), var11.getMinute(), var11.getSecond());
                  var6 = var11.getNano();
                  break;
               case LOCALDATETIME:
                  var5 = new GregorianCalendar(UTC.timeZone, Locale.US);
                  LocalDateTime var12 = (LocalDateTime)var3;
                  this.clearSetCalendar(
                     var5, true, var12.getYear(), var12.getMonthValue() - 1, var12.getDayOfMonth(), var12.getHour(), var12.getMinute(), var12.getSecond()
                  );
                  var6 = var12.getNano();
                  break;
               case OFFSETTIME:
                  OffsetTime var13 = (OffsetTime)var3;

                  try {
                     var7 = var13.getOffset().getTotalSeconds() / 60;
                  } catch (Exception var19) {
                     throw new SQLServerException(SQLServerException.getErrString("R_zoneOffsetError"), null, 0, null);
                  }

                  var6 = var13.getNano();
                  var8 = JDBCType.TIME_WITH_TIMEZONE != var4 || null != this.typeInfo && SSType.DATETIMEOFFSET != this.typeInfo.getSSType()
                     ? new SimpleTimeZone(var7 * 60 * 1000, "")
                     : UTC.timeZone;
                  String var14 = this.conn.baseYear() + "-01-01" + ' ' + var13.getHour() + ':' + var13.getMinute() + ':' + var13.getSecond();
                  var9 = Timestamp.valueOf(var14).getTime();
                  break;
               case OFFSETDATETIME:
                  OffsetDateTime var15 = (OffsetDateTime)var3;

                  try {
                     var7 = var15.getOffset().getTotalSeconds() / 60;
                  } catch (Exception var18) {
                     throw new SQLServerException(SQLServerException.getErrString("R_zoneOffsetError"), null, 0, null);
                  }

                  var6 = var15.getNano();
                  var8 = JDBCType.TIMESTAMP_WITH_TIMEZONE != var4 && JDBCType.TIME_WITH_TIMEZONE != var4
                        || null != this.typeInfo && SSType.DATETIMEOFFSET != this.typeInfo.getSSType()
                     ? new SimpleTimeZone(var7 * 60 * 1000, "")
                     : UTC.timeZone;
                  String var16 = String.format("%04d", new Object[]{var15.getYear()})
                     + '-'
                     + var15.getMonthValue()
                     + '-'
                     + var15.getDayOfMonth()
                     + ' '
                     + var15.getHour()
                     + ':'
                     + var15.getMinute()
                     + ':'
                     + var15.getSecond();
                  var9 = Timestamp.valueOf(var16).getTime();
                  break;
               case DATETIMEOFFSET:
                  DateTimeOffset var17 = (DateTimeOffset)var3;
                  var9 = var17.getTimestamp().getTime();
                  var6 = var17.getTimestamp().getNanos();
                  var7 = var17.getMinutesOffset();
                  assert null == var1.getCalendar();
                  var8 = JDBCType.DATETIMEOFFSET != var4
                        || null != this.typeInfo
                           && SSType.DATETIMEOFFSET != this.typeInfo.getSSType()
                           && SSType.VARBINARY != this.typeInfo.getSSType()
                           && SSType.VARBINARYMAX != this.typeInfo.getSSType()
                     ? new SimpleTimeZone(var7 * 60 * 1000, "")
                     : UTC.timeZone;
                  break;
               default:
                  throw new AssertionError("Unexpected JavaType: " + var2);
            }

            if (null == var5) {
               var5 = new GregorianCalendar((TimeZone)var8, Locale.US);
               var5.setLenient(true);
               var5.clear();
               var5.setTimeInMillis(var9);
            }
         }

         if (null != this.typeInfo) {
            switch (this.typeInfo.getSSType()) {
               case DATETIME2:
                  this.tdsWriter
                     .writeRPCDateTime2(
                        this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, this.typeInfo.getScale(), this.isOutParam
                     );
                  break;
               case DATE:
                  this.tdsWriter.writeRPCDate(this.name, var5, this.isOutParam);
                  break;
               case TIME:
                  this.tdsWriter.writeRPCTime(this.name, var5, var6, this.typeInfo.getScale(), this.isOutParam);
                  break;
               case DATETIMEOFFSET:
                  if (JavaType.DATETIMEOFFSET != var2) {
                     var5 = this.timestampNormalizedCalendar(this.localCalendarAsUTC(var5), var2, this.conn.baseYear());
                     var7 = 0;
                  }

                  this.tdsWriter.writeRPCDateTimeOffset(this.name, var5, var7, var6, this.typeInfo.getScale(), this.isOutParam);
                  break;
               case DATETIME:
               case SMALLDATETIME:
                  this.tdsWriter.writeRPCDateTime(this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, this.isOutParam);
                  break;
               case VARBINARY:
               case VARBINARYMAX:
                  switch (var4) {
                     case DATETIME:
                     case SMALLDATETIME:
                        this.tdsWriter
                           .writeEncryptedRPCDateTime(
                              this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, this.isOutParam, var4
                           );
                        return;
                     case TIMESTAMP:
                        assert null != DTV.this.cryptoMeta;
                        this.tdsWriter
                           .writeEncryptedRPCDateTime2(
                              this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, DTV.this.valueLength, this.isOutParam
                           );
                        return;
                     case TIME:
                        assert null != DTV.this.cryptoMeta;
                        this.tdsWriter.writeEncryptedRPCTime(this.name, var5, var6, DTV.this.valueLength, this.isOutParam);
                        return;
                     case DATE:
                        assert null != DTV.this.cryptoMeta;
                        this.tdsWriter.writeEncryptedRPCDate(this.name, var5, this.isOutParam);
                        return;
                     case TIMESTAMP_WITH_TIMEZONE:
                     case DATETIMEOFFSET:
                        if (JavaType.DATETIMEOFFSET != var2 && JavaType.OFFSETDATETIME != var2) {
                           var5 = this.timestampNormalizedCalendar(this.localCalendarAsUTC(var5), var2, this.conn.baseYear());
                           var7 = 0;
                        }

                        assert null != DTV.this.cryptoMeta;
                        this.tdsWriter.writeEncryptedRPCDateTimeOffset(this.name, var5, var7, var6, DTV.this.valueLength, this.isOutParam);
                        return;
                     default:
                        assert false : "Unexpected JDBCType: " + var4;
                        return;
                  }
               default:
                  assert false : "Unexpected SSType: " + this.typeInfo.getSSType();
            }
         } else if (this.conn.isKatmaiOrLater()) {
            if (DTV.aeLogger.isLoggable(Level.FINE) && null != DTV.this.cryptoMeta) {
               DTV.aeLogger.fine("Encrypting temporal data type.");
            }

            switch (var4) {
               case DATETIME:
               case SMALLDATETIME:
               case TIMESTAMP:
                  if (null != DTV.this.cryptoMeta) {
                     if (JDBCType.DATETIME == var4 || JDBCType.SMALLDATETIME == var4) {
                        this.tdsWriter
                           .writeEncryptedRPCDateTime(
                              this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, this.isOutParam, var4
                           );
                     } else if (0 == DTV.this.valueLength) {
                        this.tdsWriter
                           .writeEncryptedRPCDateTime2(
                              this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, this.outScale, this.isOutParam
                           );
                     } else {
                        this.tdsWriter
                           .writeEncryptedRPCDateTime2(
                              this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, DTV.this.valueLength, this.isOutParam
                           );
                     }
                  } else {
                     this.tdsWriter.writeRPCDateTime2(this.name, this.timestampNormalizedCalendar(var5, var2, this.conn.baseYear()), var6, 7, this.isOutParam);
                  }
                  break;
               case TIME:
                  if (null != DTV.this.cryptoMeta) {
                     if (0 == DTV.this.valueLength) {
                        this.tdsWriter.writeEncryptedRPCTime(this.name, var5, var6, this.outScale, this.isOutParam);
                     } else {
                        this.tdsWriter.writeEncryptedRPCTime(this.name, var5, var6, DTV.this.valueLength, this.isOutParam);
                     }
                  } else if (this.conn.getSendTimeAsDatetime()) {
                     this.tdsWriter.writeRPCDateTime(this.name, this.timestampNormalizedCalendar(var5, JavaType.TIME, 1970), var6, this.isOutParam);
                  } else {
                     this.tdsWriter.writeRPCTime(this.name, var5, var6, 7, this.isOutParam);
                  }
                  break;
               case DATE:
                  if (null != DTV.this.cryptoMeta) {
                     this.tdsWriter.writeEncryptedRPCDate(this.name, var5, this.isOutParam);
                  } else {
                     this.tdsWriter.writeRPCDate(this.name, var5, this.isOutParam);
                  }
                  break;
               case TIMESTAMP_WITH_TIMEZONE:
               case DATETIMEOFFSET:
                  if (JavaType.DATETIMEOFFSET != var2 && JavaType.OFFSETDATETIME != var2) {
                     var5 = this.timestampNormalizedCalendar(this.localCalendarAsUTC(var5), var2, this.conn.baseYear());
                     var7 = 0;
                  }

                  if (null != DTV.this.cryptoMeta) {
                     if (0 == DTV.this.valueLength) {
                        this.tdsWriter.writeEncryptedRPCDateTimeOffset(this.name, var5, var7, var6, this.outScale, this.isOutParam);
                     } else {
                        this.tdsWriter
                           .writeEncryptedRPCDateTimeOffset(this.name, var5, var7, var6, 0 == DTV.this.valueLength ? 7 : DTV.this.valueLength, this.isOutParam);
                     }
                  } else {
                     this.tdsWriter.writeRPCDateTimeOffset(this.name, var5, var7, var6, 7, this.isOutParam);
                  }
                  break;
               case TIME_WITH_TIMEZONE:
                  if (JavaType.OFFSETDATETIME != var2 && JavaType.OFFSETTIME != var2) {
                     var5 = this.timestampNormalizedCalendar(this.localCalendarAsUTC(var5), var2, this.conn.baseYear());
                     var7 = 0;
                  }

                  this.tdsWriter.writeRPCDateTimeOffset(this.name, var5, var7, var6, 7, this.isOutParam);
                  break;
               default:
                  assert false : "Unexpected JDBCType: " + var4;
            }
         } else {
            assert JDBCType.TIME == var4 || JDBCType.DATE == var4 || JDBCType.TIMESTAMP == var4 : "Unexpected JDBCType: " + var4;
            this.tdsWriter.writeRPCDateTime(this.name, this.timestampNormalizedCalendar(var5, var2, 1970), var6, this.isOutParam);
         }
      }

      private GregorianCalendar timestampNormalizedCalendar(GregorianCalendar var1, JavaType var2, int var3) {
         if (null != var1) {
            switch (var2) {
               case TIME:
               case LOCALTIME:
               case OFFSETTIME:
                  assert 1970 == var3 || 1900 == var3;
                  var1.set(var3, 0, 1);
                  break;
               case DATE:
               case LOCALDATE:
                  var1.set(11, 0);
                  var1.set(12, 0);
                  var1.set(13, 0);
                  var1.set(14, 0);
               case TIMESTAMP:
               case UTILDATE:
               case CALENDAR:
               case LOCALDATETIME:
            }
         }

         return var1;
      }

      private GregorianCalendar localCalendarAsUTC(GregorianCalendar var1) {
         if (null == var1) {
            return null;
         }

         int var2 = var1.get(1);
         int var3 = var1.get(2);
         int var4 = var1.get(5);
         int var5 = var1.get(11);
         int var6 = var1.get(12);
         int var7 = var1.get(13);
         int var8 = var1.get(14);
         var1.setTimeZone(UTC.timeZone);
         var1.set(var2, var3, var4, var5, var6, var7);
         var1.set(14, var8);
         return var1;
      }

      void execute(DTV var1, Float var2) throws SQLServerException {
         if (JDBCType.REAL == var1.getJdbcType()) {
            this.tdsWriter.writeRPCReal(this.name, var2, this.isOutParam);
         } else {
            Double var3 = null == var2 ? null : new Double(var2.floatValue());
            this.tdsWriter.writeRPCDouble(this.name, var3, this.isOutParam);
         }
      }

      void execute(DTV var1, Double var2) throws SQLServerException {
         this.tdsWriter.writeRPCDouble(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, BigDecimal var2) throws SQLServerException {
         if (DDC.exceedsMaxRPCDecimalPrecisionOrScale(var2)) {
            String var3 = var2.toString();
            this.tdsWriter.writeRPCStringUnicode(this.name, var3, this.isOutParam, this.collation);
         } else {
            this.tdsWriter.writeRPCBigDecimal(this.name, var2, this.outScale, this.isOutParam);
         }
      }

      void execute(DTV var1, Long var2) throws SQLServerException {
         this.tdsWriter.writeRPCLong(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, BigInteger var2) throws SQLServerException {
         this.tdsWriter.writeRPCLong(this.name, var2.longValue(), this.isOutParam);
      }

      void execute(DTV var1, Short var2) throws SQLServerException {
         this.tdsWriter.writeRPCShort(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, Boolean var2) throws SQLServerException {
         this.tdsWriter.writeRPCBit(this.name, var2, this.isOutParam);
      }

      void execute(DTV var1, byte[] var2) throws SQLServerException {
         if (null != DTV.this.cryptoMeta) {
            this.tdsWriter.writeRPCNameValType(this.name, this.isOutParam, TDSType.BIGVARBINARY);
            if (null != var2) {
               var2 = SQLServerSecurityUtility.encryptWithKey(var2, DTV.this.cryptoMeta, this.conn);
               this.tdsWriter.writeEncryptedRPCByteArray(var2);
               this.writeEncryptData(var1, false);
            } else {
               this.tdsWriter.writeEncryptedRPCByteArray(var2);
               this.writeEncryptData(var1, true);
            }
         } else {
            this.tdsWriter.writeRPCByteArray(this.name, var2, this.isOutParam, var1.getJdbcType(), this.collation);
         }
      }

      void writeEncryptData(DTV var1, boolean var2) throws SQLServerException {
         JDBCType var3 = null == DTV.this.jdbcTypeSetByUser ? var1.getJdbcType() : DTV.this.jdbcTypeSetByUser;
         switch (var3.getIntValue()) {
            case -148:
            case -146:
            case 2:
            case 3:
               if (JDBCType.MONEY != var3 && JDBCType.SMALLMONEY != var3) {
                  this.tdsWriter.writeByte(TDSType.NUMERICN.byteValue());
                  if (var2) {
                     this.tdsWriter.writeByte((byte)17);
                     if (null != DTV.this.cryptoMeta && null != DTV.this.cryptoMeta.getBaseTypeInfo()) {
                        this.tdsWriter
                           .writeByte((byte)(0 != DTV.this.valueLength ? DTV.this.valueLength : DTV.this.cryptoMeta.getBaseTypeInfo().getPrecision()));
                     } else {
                        this.tdsWriter.writeByte((byte)(0 != DTV.this.valueLength ? DTV.this.valueLength : 18));
                     }

                     this.tdsWriter.writeByte((byte)(0 != this.outScale ? this.outScale : 0));
                  } else {
                     this.tdsWriter.writeByte((byte)17);
                     if (null != DTV.this.cryptoMeta && null != DTV.this.cryptoMeta.getBaseTypeInfo()) {
                        this.tdsWriter.writeByte((byte)DTV.this.cryptoMeta.getBaseTypeInfo().getPrecision());
                     } else {
                        this.tdsWriter.writeByte((byte)(0 != DTV.this.valueLength ? DTV.this.valueLength : 18));
                     }

                     if (null != DTV.this.cryptoMeta && null != DTV.this.cryptoMeta.getBaseTypeInfo()) {
                        this.tdsWriter.writeByte((byte)DTV.this.cryptoMeta.getBaseTypeInfo().getScale());
                     } else {
                        this.tdsWriter.writeByte((byte)(null != var1.getScale() ? var1.getScale() : 0));
                     }
                  }
               } else {
                  this.tdsWriter.writeByte(TDSType.MONEYN.byteValue());
                  this.tdsWriter.writeByte((byte)(JDBCType.MONEY == var3 ? 8 : 4));
               }
               break;
            case -145:
               this.tdsWriter.writeByte(TDSType.GUID.byteValue());
               if (var2) {
                  this.tdsWriter.writeByte((byte)(0 != DTV.this.valueLength ? DTV.this.valueLength : 1));
               } else {
                  this.tdsWriter.writeByte((byte)16);
               }
               break;
            case -16:
            case -9:
               this.tdsWriter.writeByte(TDSType.NVARCHAR.byteValue());
               if (var2) {
                  if (var1.jdbcTypeSetByUser.getIntValue() == -16) {
                     this.tdsWriter.writeShort((short)-1);
                  } else {
                     this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength * 2 : 1));
                  }
               } else if (this.isOutParam) {
                  if (var1.jdbcTypeSetByUser.getIntValue() == -16) {
                     this.tdsWriter.writeShort((short)-1);
                  } else {
                     this.tdsWriter.writeShort((short)(DTV.this.valueLength * 2));
                  }
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }

               if (null != this.collation) {
                  this.collation.writeCollation(this.tdsWriter);
               } else {
                  this.conn.getDatabaseCollation().writeCollation(this.tdsWriter);
               }
               break;
            case -15:
               this.tdsWriter.writeByte(TDSType.NCHAR.byteValue());
               if (var2) {
                  this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength * 2 : 1));
               } else if (this.isOutParam) {
                  this.tdsWriter.writeShort((short)(DTV.this.valueLength * 2));
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }

               if (null != this.collation) {
                  this.collation.writeCollation(this.tdsWriter);
               } else {
                  this.conn.getDatabaseCollation().writeCollation(this.tdsWriter);
               }
               break;
            case -7:
               this.tdsWriter.writeByte(TDSType.BITN.byteValue());
               this.tdsWriter.writeByte((byte)1);
               break;
            case -6:
               this.tdsWriter.writeByte(TDSType.INTN.byteValue());
               this.tdsWriter.writeByte((byte)1);
               break;
            case -5:
               this.tdsWriter.writeByte(TDSType.INTN.byteValue());
               this.tdsWriter.writeByte((byte)8);
               break;
            case -4:
            case -3:
               this.tdsWriter.writeByte(TDSType.BIGVARBINARY.byteValue());
               if (var2) {
                  if (var1.jdbcTypeSetByUser.getIntValue() == -4) {
                     this.tdsWriter.writeShort((short)-1);
                  } else {
                     this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength : 1));
                  }
               } else if (var1.jdbcTypeSetByUser.getIntValue() == -4) {
                  this.tdsWriter.writeShort((short)-1);
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }
               break;
            case -2:
               this.tdsWriter.writeByte(TDSType.BIGBINARY.byteValue());
               if (var2) {
                  this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength : 1));
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }
               break;
            case -1:
            case 12:
               this.tdsWriter.writeByte(TDSType.BIGVARCHAR.byteValue());
               if (var2) {
                  if (var1.jdbcTypeSetByUser.getIntValue() == -1) {
                     this.tdsWriter.writeShort((short)-1);
                  } else {
                     this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength : 1));
                  }
               } else if (var1.getJdbcType().getIntValue() == -1 || var1.getJdbcType().getIntValue() == -16) {
                  this.tdsWriter.writeShort((short)1);
               } else if (var1.jdbcTypeSetByUser.getIntValue() == -1) {
                  this.tdsWriter.writeShort((short)-1);
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }

               if (null != this.collation) {
                  this.collation.writeCollation(this.tdsWriter);
               } else {
                  this.conn.getDatabaseCollation().writeCollation(this.tdsWriter);
               }
               break;
            case 1:
               this.tdsWriter.writeByte(TDSType.BIGCHAR.byteValue());
               if (var2) {
                  this.tdsWriter.writeShort((short)(0 != DTV.this.valueLength ? DTV.this.valueLength : 1));
               } else {
                  this.tdsWriter.writeShort((short)DTV.this.valueLength);
               }

               if (null != this.collation) {
                  this.collation.writeCollation(this.tdsWriter);
               } else {
                  this.conn.getDatabaseCollation().writeCollation(this.tdsWriter);
               }
               break;
            case 4:
               this.tdsWriter.writeByte(TDSType.INTN.byteValue());
               this.tdsWriter.writeByte((byte)4);
               break;
            case 5:
               this.tdsWriter.writeByte(TDSType.INTN.byteValue());
               this.tdsWriter.writeByte((byte)2);
               break;
            case 7:
               this.tdsWriter.writeByte(TDSType.FLOATN.byteValue());
               this.tdsWriter.writeByte((byte)4);
               break;
            case 8:
               this.tdsWriter.writeByte(TDSType.FLOATN.byteValue());
               this.tdsWriter.writeByte((byte)8);
               break;
            default:
               MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
               throw new SQLServerException(var4.format(new Object[]{var3}), null, 0, null);
         }

         this.tdsWriter.writeCryptoMetaData();
      }

      void execute(DTV var1, Blob var2) throws SQLServerException {
         assert null != var2;
         long var3 = 0L;
         InputStream var5 = null;

         try {
            var3 = DataTypes.getCheckedLength(this.conn, var1.getJdbcType(), var2.length(), false);
            var5 = var2.getBinaryStream();
         } catch (SQLException var7) {
            SQLServerException.makeFromDriverError(this.conn, null, var7.getMessage(), null, false);
         }

         if (null == var5) {
            this.tdsWriter.writeRPCByteArray(this.name, null, this.isOutParam, var1.getJdbcType(), this.collation);
         } else {
            this.tdsWriter.writeRPCInputStream(this.name, var5, var3, this.isOutParam, var1.getJdbcType(), this.collation);
         }
      }

      void execute(DTV var1, SQLServerSQLXML var2) throws SQLServerException {
         InputStream var3 = null == var2 ? null : var2.getValue();
         this.tdsWriter.writeRPCXML(this.name, var3, null == var3 ? 0L : var1.getStreamSetterArgs().getLength(), this.isOutParam);
      }

      void execute(DTV var1, InputStream var2) throws SQLServerException {
         this.tdsWriter
            .writeRPCInputStream(
               this.name, var2, null == var2 ? 0L : var1.getStreamSetterArgs().getLength(), this.isOutParam, var1.getJdbcType(), this.collation
            );
      }

      void execute(DTV var1, Reader var2) throws SQLServerException {
         JDBCType var3 = var1.getJdbcType();
         assert null != var2;
         assert JDBCType.NCHAR == var3 || JDBCType.NVARCHAR == var3 || JDBCType.LONGNVARCHAR == var3 || JDBCType.NCLOB == var3 : "SendByRPCOp(Reader): Unexpected JDBC type "
            + var3;
         this.tdsWriter.writeRPCReaderUnicode(this.name, var2, var1.getStreamSetterArgs().getLength(), this.isOutParam, this.collation);
      }
   }
}
