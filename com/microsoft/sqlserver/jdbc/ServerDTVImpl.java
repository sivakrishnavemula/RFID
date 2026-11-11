package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ServerDTVImpl extends DTVImpl {
   private int valueLength;
   private TDSReaderMark valueMark;
   private boolean isNull;
   private static final int STREAMCONSUMED = -2;
   private static final Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.DTV");

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
      var1.setImpl(new AppDTVImpl());
      var1.setValue(var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   void setValue(Object var1, JavaType var2) {
      assert false;
   }

   void setPositionAfterStreamed(TDSReader var1) {
      this.valueMark = var1.mark();
      this.valueLength = -2;
   }

   void setStreamSetterArgs(StreamSetterArgs var1) {
      assert false;
   }

   void setCalendar(Calendar var1) {
      assert false;
   }

   void setScale(Integer var1) {
      assert false;
   }

   void setForceEncrypt(boolean var1) {
      assert false;
   }

   StreamSetterArgs getStreamSetterArgs() {
      assert false;
      return null;
   }

   Calendar getCalendar() {
      assert false;
      return null;
   }

   Integer getScale() {
      assert false;
      return null;
   }

   boolean isNull() {
      return this.isNull;
   }

   void setJdbcType(JDBCType var1) {
      assert false;
   }

   JDBCType getJdbcType() {
      assert false;
      return JDBCType.UNKNOWN;
   }

   JavaType getJavaType() {
      assert false;
      return JavaType.OBJECT;
   }

   final void initFromCompressedNull() {
      assert this.valueMark == null;
      this.isNull = true;
   }

   final void skipValue(TypeInfo var1, TDSReader var2, boolean var3) throws SQLServerException {
      if (null != this.valueMark || !this.isNull) {
         if (null == this.valueMark) {
            this.getValuePrep(var1, var2);
         }

         var2.reset(this.valueMark);
         if (this.valueLength != -2) {
            if (this.valueLength == -1) {
               assert SSLenType.PARTLENTYPE == var1.getSSLenType();
               PLPInputStream var4 = PLPInputStream.makeTempStream(var2, var3, this);

               try {
                  if (null != var4) {
                     var4.close();
                  }
               } catch (IOException var6) {
                  var2.getConnection().terminate(3, var6.getMessage());
               }
            } else {
               assert this.valueLength >= 0;
               var2.skip(this.valueLength);
            }
         }
      }
   }

   private final void getValuePrep(TypeInfo var1, TDSReader var2) throws SQLServerException {
      assert null == this.valueMark;
      switch (var1.getSSLenType()) {
         case PARTLENTYPE:
            this.valueLength = -1;
            this.isNull = PLPInputStream.isNull(var2);
            break;
         case FIXEDLENTYPE:
            this.valueLength = var1.getMaxLength();
            this.isNull = 0 == this.valueLength;
            break;
         case BYTELENTYPE:
            this.valueLength = var2.readUnsignedByte();
            this.isNull = 0 == this.valueLength;
            break;
         case USHORTLENTYPE:
            this.valueLength = var2.readUnsignedShort();
            this.isNull = 65535 == this.valueLength;
            if (this.isNull) {
               this.valueLength = 0;
            }
            break;
         case LONGLENTYPE:
            if (SSType.TEXT != var1.getSSType() && SSType.IMAGE != var1.getSSType() && SSType.NTEXT != var1.getSSType()) {
               this.valueLength = var2.readInt();
               this.isNull = 0 == this.valueLength;
            } else {
               this.isNull = 0 == var2.readUnsignedByte();
               if (this.isNull) {
                  this.valueLength = 0;
               } else {
                  var2.skip(24);
                  this.valueLength = var2.readInt();
               }
            }
      }

      if (this.valueLength > var1.getMaxLength()) {
         var2.throwInvalidTDS();
      }

      this.valueMark = var2.mark();
   }

   Object denormalizedValue(byte[] var1, JDBCType var2, TypeInfo var3, SQLServerConnection var4, InputStreamGetterArgs var5, byte var6, Calendar var7) throws SQLServerException {
      if (1 != var6) {
         MessageFormat var19 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedNormalizationVersionAE"));
         throw new SQLServerException(var19.format(new Object[]{var6, 1}), null, 0, null);
      }

      if (aeLogger.isLoggable(Level.FINE)) {
         aeLogger.fine("Denormalizing decrypted data based on its SQL Server type(" + var3.getSSType() + ") and JDBC type(" + var2 + ").");
      }

      SSType var8 = var3.getSSType();
      switch (var8) {
         case CHAR:
         case VARCHAR:
         case NCHAR:
         case NVARCHAR:
         case VARCHARMAX:
         case NVARCHARMAX:
            try {
               String var31 = new String(var1, 0, var1.length, null == var3.getCharset() ? var4.getDatabaseCollation().getCharset() : var3.getCharset());
               if (SSType.CHAR == var8 || SSType.NCHAR == var8) {
                  StringBuffer var35 = new StringBuffer(var31);
                  int var37 = var3.getPrecision() - var31.length();

                  for (int var39 = 0; var39 < var37; var39++) {
                     var35.append(' ');
                  }

                  var31 = var35.toString();
               }

               return DDC.convertStringToObject(var31, var3.getCharset(), var2, var5.streamType);
            } catch (IllegalArgumentException var17) {
               MessageFormat var34 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
               throw new SQLServerException(var34.format(new Object[]{var8, var2}), null, 0, var17);
            } catch (UnsupportedEncodingException var18) {
               MessageFormat var33 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
               throw new SQLServerException(var33.format(new Object[]{var3.getCharset()}), null, 0, null);
            }
         case BIT:
         case TINYINT:
         case SMALLINT:
         case INTEGER:
         case BIGINT:
            if (8 != var1.length) {
               MessageFormat var30 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var30.format(new Object[]{var8}), null, 0, null);
            }

            return DDC.convertLongToObject(Util.readLong(var1, 0), var2, var8, var5.streamType);
         case REAL:
         case FLOAT:
            if (8 == var1.length) {
               return DDC.convertDoubleToObject(
                  ByteBuffer.wrap(var1).order(ByteOrder.LITTLE_ENDIAN).getDouble(), JDBCType.VARBINARY == var2 ? var8.getJDBCType() : var2, var5.streamType
               );
            } else {
               if (4 == var1.length) {
                  return DDC.convertFloatToObject(
                     ByteBuffer.wrap(var1).order(ByteOrder.LITTLE_ENDIAN).getFloat(), JDBCType.VARBINARY == var2 ? var8.getJDBCType() : var2, var5.streamType
                  );
               }

               MessageFormat var29 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var29.format(new Object[]{var8}), null, 0, null);
            }
         case SMALLMONEY:
            return DDC.convertMoneyToObject(
               new BigDecimal(BigInteger.valueOf(Util.readInt(var1, 4)), 4), JDBCType.VARBINARY == var2 ? var8.getJDBCType() : var2, var5.streamType, 4
            );
         case MONEY:
            BigInteger var28 = BigInteger.valueOf((long)Util.readInt(var1, 0) << 32 | Util.readInt(var1, 4) & 4294967295L);
            return DDC.convertMoneyToObject(new BigDecimal(var28, 4), JDBCType.VARBINARY == var2 ? var8.getJDBCType() : var2, var5.streamType, 8);
         case NUMERIC:
         case DECIMAL:
            return DDC.convertBigDecimalToObject(
               Util.readBigDecimal(var1, var1.length, var3.getScale()), JDBCType.VARBINARY == var2 ? var8.getJDBCType() : var2, var5.streamType
            );
         case BINARY:
         case VARBINARY:
         case VARBINARYMAX:
            return DDC.convertBytesToObject(var1, var2, var3);
         case DATE:
            if (3 != var1.length) {
               MessageFormat var27 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var27.format(new Object[]{var8}), null, 0, null);
            }

            int var26 = this.getDaysIntoCE(var1, var8);
            return DDC.convertTemporalToObject(var2, var8, var7, var26, 0L, 0);
         case TIME:
            long var25 = this.readNanosSinceMidnightAE(var1, var3.getScale(), var8);
            return DDC.convertTemporalToObject(var2, SSType.TIME, var7, 0, var25, var3.getScale());
         case DATETIME2:
            if (8 != var1.length) {
               MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var24.format(new Object[]{var8}), null, 0, null);
            }

            int var23 = var1.length - 3;
            byte[] var32 = new byte[var23];
            byte[] var36 = new byte[3];
            System.arraycopy(var1, 0, var32, 0, var23);
            System.arraycopy(var1, var23, var36, 0, 3);
            long var38 = this.readNanosSinceMidnightAE(var32, var3.getScale(), var8);
            int var14 = this.getDaysIntoCE(var36, var8);
            return DDC.convertTemporalToObject(var2, SSType.DATETIME2, var7, var14, var38, var3.getScale());
         case SMALLDATETIME:
            if (4 != var1.length) {
               MessageFormat var22 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var22.format(new Object[]{var8}), null, 0, null);
            }

            return DDC.convertTemporalToObject(var2, SSType.DATETIME, var7, Util.readUnsignedShort(var1, 0), Util.readUnsignedShort(var1, 2) * 60L * 1000L, 0);
         case DATETIME:
            if (8 != var1.length) {
               MessageFormat var21 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
               throw new SQLServerException(var21.format(new Object[]{var8}), null, 0, null);
            }

            return DDC.convertTemporalToObject(var2, SSType.DATETIME, var7, Util.readInt(var1, 0), (Util.readInt(var1, 4) * 10 + 1) / 3, 0);
         case DATETIMEOFFSET:
            int var20 = var1.length - 5;
            byte[] var10 = new byte[var20];
            byte[] var11 = new byte[3];
            byte[] var12 = new byte[2];
            System.arraycopy(var1, 0, var10, 0, var20);
            System.arraycopy(var1, var20, var11, 0, 3);
            System.arraycopy(var1, var20 + 3, var12, 0, 2);
            long var13 = this.readNanosSinceMidnightAE(var10, var3.getScale(), var8);
            int var15 = this.getDaysIntoCE(var11, var8);
            short var16 = ByteBuffer.wrap(var12).order(ByteOrder.LITTLE_ENDIAN).getShort();
            return DDC.convertTemporalToObject(
               var2, SSType.DATETIMEOFFSET, new GregorianCalendar(new SimpleTimeZone(var16 * 60 * 1000, ""), Locale.US), var15, var13, var3.getScale()
            );
         case GUID:
            return Util.readGUID(var1);
         default:
            MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
            throw new SQLServerException(var9.format(new Object[]{var8}), null, 0, null);
      }
   }

   Object getValue(DTV var1, JDBCType var2, int var3, InputStreamGetterArgs var4, Calendar var5, TypeInfo var6, CryptoMetadata var7, TDSReader var8) throws SQLServerException {
      SQLServerConnection var9 = var8.getConnection();
      Object var10 = null;
      byte[] var11 = null;
      boolean var12 = false;
      SSType var13 = var6.getSSType();
      if (null != var7) {
         assert SSType.VARBINARY == var6.getSSType() || SSType.VARBINARYMAX == var6.getSSType();
         var13 = var7.baseTypeInfo.getSSType();
         var12 = true;
         if (aeLogger.isLoggable(Level.FINE)) {
            aeLogger.fine("Data is encrypted, SQL Server Data Type: " + var13 + ", Encryption Type: " + var7.getEncryptionType());
         }
      }

      if (null == this.valueMark && !this.isNull) {
         this.getValuePrep(var6, var8);
      }

      assert this.valueMark != null || this.valueMark == null && this.isNull;
      boolean var14 = false;
      if (null != var4) {
         if (!var4.streamType.convertsFrom(var6)) {
            DataTypes.throwConversionError(var6.getSSType().toString(), var4.streamType.toString());
         }
      } else {
         if (!var13.convertsTo(var2)) {
            if (var12) {
               if (!Util.isBinaryType(var2.getIntValue())) {
                  DataTypes.throwConversionError(var13.toString(), var2.toString());
               }
            } else {
               DataTypes.throwConversionError(var13.toString(), var2.toString());
            }
         }

         var4 = InputStreamGetterArgs.getDefaultArgs();
      }

      if (-2 == this.valueLength) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_dataAlreadyAccessed"), null, 0, false);
      }

      if (!this.isNull) {
         var8.reset(this.valueMark);
         if (var12) {
            if (-1 == this.valueLength) {
               var10 = DDC.convertStreamToObject(PLPInputStream.makeStream(var8, var4, this), var6, JDBCType.VARBINARY, var4);
            } else {
               var10 = DDC.convertStreamToObject(new SimpleInputStream(var8, this.valueLength, var4, this), var6, JDBCType.VARBINARY, var4);
            }

            if (aeLogger.isLoggable(Level.FINE)) {
               aeLogger.fine("Encrypted data is retrieved.");
            }

            if (var10 instanceof SimpleInputStream) {
               throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), null);
            }

            var11 = SQLServerSecurityUtility.decryptWithKey((byte[])var10, var7, var9);
            return this.denormalizedValue(var11, var2, var7.baseTypeInfo, var9, var4, var7.normalizationRuleVersion, var5);
         }

         label106:
         switch (var13) {
            case CHAR:
            case VARCHAR:
            case NCHAR:
            case NVARCHAR:
            case BINARY:
            case VARBINARY:
            case TEXT:
            case NTEXT:
            case IMAGE:
            case TIMESTAMP:
               var10 = DDC.convertStreamToObject(new SimpleInputStream(var8, this.valueLength, var4, this), var6, var2, var4);
               break;
            case VARCHARMAX:
            case NVARCHARMAX:
            case VARBINARYMAX:
            case UDT:
               var10 = DDC.convertStreamToObject(PLPInputStream.makeStream(var8, var4, this), var6, var2, var4);
               break;
            case BIT:
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
               switch (this.valueLength) {
                  case 1:
                     var10 = DDC.convertIntegerToObject(var8.readUnsignedByte(), this.valueLength, var2, var4.streamType);
                     break label106;
                  case 2:
                     var10 = DDC.convertIntegerToObject(var8.readShort(), this.valueLength, var2, var4.streamType);
                     break label106;
                  case 3:
                  case 5:
                  case 6:
                  case 7:
                  default:
                     assert false : "Unexpected valueLength" + this.valueLength;
                     break label106;
                  case 4:
                     var10 = DDC.convertIntegerToObject(var8.readInt(), this.valueLength, var2, var4.streamType);
                     break label106;
                  case 8:
                     var10 = DDC.convertLongToObject(var8.readLong(), var2, var13, var4.streamType);
                     break label106;
               }
            case REAL:
               var10 = var8.readReal(this.valueLength, var2, var4.streamType);
               break;
            case FLOAT:
               var10 = var8.readFloat(this.valueLength, var2, var4.streamType);
               break;
            case SMALLMONEY:
            case MONEY:
               var10 = var8.readMoney(this.valueLength, var2, var4.streamType);
               break;
            case NUMERIC:
            case DECIMAL:
               var10 = var8.readDecimal(this.valueLength, var6, var2, var4.streamType);
               break;
            case DATE:
               var10 = var8.readDate(this.valueLength, var5, var2);
               break;
            case TIME:
               var10 = var8.readTime(this.valueLength, var6, var5, var2);
               break;
            case DATETIME2:
               var10 = var8.readDateTime2(this.valueLength, var6, var5, var2);
               break;
            case SMALLDATETIME:
            case DATETIME:
               var10 = var8.readDateTime(this.valueLength, var5, var2, var4.streamType);
               break;
            case DATETIMEOFFSET:
               var10 = var8.readDateTimeOffset(this.valueLength, var6, var2);
               break;
            case GUID:
               var10 = var8.readGUID(this.valueLength, var2, var4.streamType);
               break;
            case XML:
               var10 = DDC.convertStreamToObject(
                  (BaseInputStream)(!var2.isBinary() && var2 != JDBCType.SQLXML
                     ? PLPInputStream.makeStream(var8, var4, this)
                     : PLPXMLInputStream.makeXMLStream(var8, var4, this)),
                  var6,
                  var2,
                  var4
               );
               break;
            default:
               assert false : "Unexpected SSType " + var6.getSSType();
         }
      }

      assert this.isNull || null != var10;
      return var10;
   }

   Object getSetterValue() {
      assert false;
      return null;
   }

   private long readNanosSinceMidnightAE(byte[] var1, int var2, SSType var3) throws SQLServerException {
      long var4 = 0L;

      for (int var6 = 0; var6 < var1.length; var6++) {
         var4 |= (var1[var6] & 255L) << 8 * var6;
      }

      if (0L <= var4 && var4 < 864000000000L) {
         return 100L * var4;
      }

      MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
      throw new SQLServerException(var7.format(new Object[]{var3}), null, 0, null);
   }

   private int getDaysIntoCE(byte[] var1, SSType var2) throws SQLServerException {
      int var3 = 0;

      for (int var4 = 0; var4 < var1.length; var4++) {
         var3 |= (var1[var4] & 255) << 8 * var4;
      }

      if (var3 < 0) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_NormalizationErrorAE"));
         throw new SQLServerException(var5.format(new Object[]{var2}), null, 0, null);
      } else {
         return var3;
      }
   }
}
