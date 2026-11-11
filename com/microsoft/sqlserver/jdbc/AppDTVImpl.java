package com.microsoft.sqlserver.jdbc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import microsoft.sql.DateTimeOffset;

final class AppDTVImpl extends DTVImpl {
   private JDBCType jdbcType = JDBCType.UNKNOWN;
   private Object value;
   private JavaType javaType;
   private StreamSetterArgs streamSetterArgs;
   private Calendar cal;
   private Integer scale;
   private boolean forceEncrypt;

   final void skipValue(TypeInfo var1, TDSReader var2, boolean var3) throws SQLServerException {
      assert false;
   }

   final void initFromCompressedNull() {
      assert false;
   }

   void setValue(
      DTV var1,
      SQLCollation var2,
      JDBCType var3,
      Object var4,
      JavaType var5,
      StreamSetterArgs var6,
      Calendar var7,
      Integer var8,
      SQLServerConnection var9,
      boolean var10
   ) throws SQLServerException {
      var1.setValue(var4, var5);
      var1.setJdbcType(var3);
      var1.setStreamSetterArgs(var6);
      var1.setCalendar(var7);
      var1.setScale(var8);
      var1.setForceEncrypt(var10);
      var1.executeOp(new AppDTVImpl.SetValueOp(var2, var9));
   }

   void setValue(Object var1, JavaType var2) {
      this.value = var1;
      this.javaType = var2;
   }

   void setStreamSetterArgs(StreamSetterArgs var1) {
      this.streamSetterArgs = var1;
   }

   void setCalendar(Calendar var1) {
      this.cal = var1;
   }

   void setScale(Integer var1) {
      this.scale = var1;
   }

   void setForceEncrypt(boolean var1) {
      this.forceEncrypt = var1;
   }

   StreamSetterArgs getStreamSetterArgs() {
      return this.streamSetterArgs;
   }

   Calendar getCalendar() {
      return this.cal;
   }

   Integer getScale() {
      return this.scale;
   }

   boolean isNull() {
      return null == this.value;
   }

   void setJdbcType(JDBCType var1) {
      this.jdbcType = var1;
   }

   JDBCType getJdbcType() {
      return this.jdbcType;
   }

   JavaType getJavaType() {
      return this.javaType;
   }

   Object getValue(DTV var1, JDBCType var2, int var3, InputStreamGetterArgs var4, Calendar var5, TypeInfo var6, CryptoMetadata var7, TDSReader var8) throws SQLServerException {
      if (this.jdbcType != var2) {
         DataTypes.throwConversionError(this.jdbcType.toString(), var2.toString());
      }

      return this.value;
   }

   Object getSetterValue() {
      return this.value;
   }

   final class SetValueOp extends DTVExecuteOp {
      private final SQLCollation collation;
      private final SQLServerConnection con;

      SetValueOp(SQLCollation var2, SQLServerConnection var3) {
         this.collation = var2;
         this.con = var3;
      }

      void execute(DTV var1, String var2) throws SQLServerException {
         JDBCType var3 = var1.getJdbcType();
         if (JDBCType.DECIMAL != var3 && JDBCType.NUMERIC != var3 && JDBCType.MONEY != var3 && JDBCType.SMALLMONEY != var3) {
            if (var3.isBinary()) {
               assert null != var2;
               var1.setValue(ParameterUtils.HexToBin(var2), JavaType.BYTEARRAY);
            } else if (null != this.collation && (JDBCType.CHAR == var3 || JDBCType.VARCHAR == var3 || JDBCType.LONGVARCHAR == var3 || JDBCType.CLOB == var3)) {
               byte[] var4 = null;
               if (null != var2) {
                  try {
                     var4 = var2.getBytes(this.collation.getCharset());
                  } catch (UnsupportedEncodingException var8) {
                     MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_encodingErrorWritingTDS"));
                     Object[] var7 = new Object[]{new String(var8.getMessage())};
                     SQLServerException.makeFromDriverError(this.con, null, var6.format(var7), null, true);
                  }
               }

               var1.setValue(var4, JavaType.BYTEARRAY);
            }
         } else {
            assert null != var2;

            try {
               var1.setValue(new BigDecimal(var2), JavaType.BIGDECIMAL);
            } catch (NumberFormatException var9) {
               DataTypes.throwConversionError("String", var3.toString());
            }
         }
      }

      void execute(DTV var1, Clob var2) throws SQLServerException {
         assert null != var2;

         try {
            DataTypes.getCheckedLength(this.con, var1.getJdbcType(), var2.length(), false);
         } catch (SQLException var4) {
            SQLServerException.makeFromDriverError(this.con, null, var4.getMessage(), null, false);
         }
      }

      void execute(DTV var1, SQLServerSQLXML var2) throws SQLServerException {
      }

      void execute(DTV var1, Byte var2) throws SQLServerException {
      }

      void execute(DTV var1, Integer var2) throws SQLServerException {
      }

      void execute(DTV var1, Time var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, Date var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, Timestamp var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, java.util.Date var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, LocalDate var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, LocalTime var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, LocalDateTime var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, OffsetTime var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, OffsetDateTime var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, Calendar var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, DateTimeOffset var2) throws SQLServerException {
         if (var1.getJdbcType().isTextual()) {
            assert var2 != null : "value is null";
            var1.setValue(var2.toString(), JavaType.STRING);
         }
      }

      void execute(DTV var1, TVP var2) throws SQLServerException {
      }

      void execute(DTV var1, Float var2) throws SQLServerException {
      }

      void execute(DTV var1, Double var2) throws SQLServerException {
      }

      void execute(DTV var1, BigDecimal var2) throws SQLServerException {
         if (null != var2) {
            Integer var3 = var1.getScale();
            if (null != var3 && var3 != var2.scale()) {
               var2 = var2.setScale(var3, 1);
            }
         }

         var1.setValue(var2, JavaType.BIGDECIMAL);
      }

      void execute(DTV var1, Long var2) throws SQLServerException {
      }

      void execute(DTV var1, BigInteger var2) throws SQLServerException {
      }

      void execute(DTV var1, Short var2) throws SQLServerException {
      }

      void execute(DTV var1, Boolean var2) throws SQLServerException {
      }

      void execute(DTV var1, byte[] var2) throws SQLServerException {
      }

      void execute(DTV var1, Blob var2) throws SQLServerException {
         assert null != var2;

         try {
            DataTypes.getCheckedLength(this.con, var1.getJdbcType(), var2.length(), false);
         } catch (SQLException var4) {
            SQLServerException.makeFromDriverError(this.con, null, var4.getMessage(), null, false);
         }
      }

      void execute(DTV var1, InputStream var2) throws SQLServerException {
         DataTypes.getCheckedLength(this.con, var1.getJdbcType(), var1.getStreamSetterArgs().getLength(), true);
         if (JDBCType.NCHAR == AppDTVImpl.this.jdbcType || JDBCType.NVARCHAR == AppDTVImpl.this.jdbcType || JDBCType.LONGNVARCHAR == AppDTVImpl.this.jdbcType) {
            Reader var3 = null;

            try {
               var3 = new InputStreamReader(var2, "US-ASCII");
            } catch (UnsupportedEncodingException var5) {
               throw new SQLServerException(null, var5.getMessage(), null, 0, true);
            }

            var1.setValue(var3, JavaType.READER);
            this.execute(var1, var3);
         }
      }

      void execute(DTV var1, Reader var2) throws SQLServerException {
         assert null != var2;
         JDBCType var3 = var1.getJdbcType();
         long var4 = DataTypes.getCheckedLength(this.con, var1.getJdbcType(), var1.getStreamSetterArgs().getLength(), true);
         if (var3.isBinary()) {
            String var6 = DDC.convertReaderToString(var2, (int)var4);
            if (-1L != var4 && var6.length() != var4) {
               MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
               Object[] var8 = new Object[]{var4, var6.length()};
               SQLServerException.makeFromDriverError(null, null, var7.format(var8), "", true);
            }

            var1.setValue(var6, JavaType.STRING);
            this.execute(var1, var6);
         } else if (null != this.collation && (JDBCType.CHAR == var3 || JDBCType.VARCHAR == var3 || JDBCType.LONGVARCHAR == var3 || JDBCType.CLOB == var3)) {
            ReaderInputStream var11 = null;

            try {
               var11 = new ReaderInputStream(var2, this.collation.getCharset(), var4);
            } catch (UnsupportedEncodingException var10) {
               MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_encodingErrorWritingTDS"));
               Object[] var9 = new Object[]{new String(var10.getMessage())};
               SQLServerException.makeFromDriverError(this.con, null, var12.format(var9), null, true);
            }

            var1.setValue(var11, JavaType.INPUTSTREAM);
            var1.setStreamSetterArgs(new StreamSetterArgs(StreamType.CHARACTER, -1L));
            this.execute(var1, var11);
         }
      }
   }
}
