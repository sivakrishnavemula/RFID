package com.microsoft.sqlserver.jdbc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import microsoft.sql.DateTimeOffset;

final class DDC {
   private static final BigInteger maxRPCDecimalValue = new BigInteger("99999999999999999999999999999999999999");

   static final Object convertIntegerToObject(int var0, int var1, JDBCType var2, StreamType var3) {
      switch (var2) {
         case INTEGER:
            return new Integer(var0);
         case SMALLINT:
         case TINYINT:
            return new Short((short)var0);
         case BIT:
         case BOOLEAN:
            return new Boolean(0 != var0);
         case BIGINT:
            return new Long(var0);
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return new BigDecimal(Integer.toString(var0));
         case FLOAT:
         case DOUBLE:
            return new Double(var0);
         case REAL:
            return new Float(var0);
         case BINARY:
            return convertIntToBytes(var0, var1);
         default:
            return Integer.toString(var0);
      }
   }

   static final Object convertLongToObject(long var0, JDBCType var2, SSType var3, StreamType var4) {
      switch (var2) {
         case INTEGER:
            return new Integer((int)var0);
         case SMALLINT:
         case TINYINT:
            return new Short((short)var0);
         case BIT:
         case BOOLEAN:
            return new Boolean(0L != var0);
         case BIGINT:
            return new Long(var0);
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return new BigDecimal(Long.toString(var0));
         case FLOAT:
         case DOUBLE:
            return new Double(var0);
         case REAL:
            return new Float((float)var0);
         case BINARY:
            byte[] var5 = convertLongToBytes(var0);
            byte var6 = 0;
            switch (var3) {
               case BIT:
               case TINYINT:
                  var6 = 1;
                  byte[] var14 = new byte[var6];
                  System.arraycopy(var5, var5.length - var6, var14, 0, var6);
                  return var14;
               case SMALLINT:
                  var6 = 2;
                  byte[] var13 = new byte[var6];
                  System.arraycopy(var5, var5.length - var6, var13, 0, var6);
                  return var13;
               case INTEGER:
                  var6 = 4;
                  byte[] var12 = new byte[var6];
                  System.arraycopy(var5, var5.length - var6, var12, 0, var6);
                  return var12;
               case BIGINT:
                  var6 = 8;
                  byte[] var7 = new byte[var6];
                  System.arraycopy(var5, var5.length - var6, var7, 0, var6);
                  return var7;
               default:
                  return var5;
            }
         case VARBINARY:
            switch (var3) {
               case BIT:
                  return new Boolean(0L != var0);
               case TINYINT:
               case SMALLINT:
                  return new Short((short)var0);
               case INTEGER:
                  return new Integer((int)var0);
               case BIGINT:
                  return new Long(var0);
               case DECIMAL:
               case NUMERIC:
               case MONEY:
               case SMALLMONEY:
                  return new BigDecimal(Long.toString(var0));
               case FLOAT:
                  return new Double(var0);
               case REAL:
                  return new Float((float)var0);
               case BINARY:
                  return convertLongToBytes(var0);
               default:
                  return Long.toString(var0);
            }
         default:
            return Long.toString(var0);
      }
   }

   static final byte[] convertIntToBytes(int var0, int var1) {
      byte[] var2 = new byte[var1];
      int var3 = var1;

      while (var3-- > 0) {
         var2[var3] = (byte)(var0 & 0xFF);
         var0 >>= 8;
      }

      return var2;
   }

   static final Object convertFloatToObject(float var0, JDBCType var1, StreamType var2) {
      switch (var1) {
         case INTEGER:
            return new Integer((int)var0);
         case SMALLINT:
         case TINYINT:
            return new Short((short)var0);
         case BIT:
         case BOOLEAN:
            return new Boolean(0 != Float.compare(0.0F, var0));
         case BIGINT:
            return new Long((long)var0);
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return new BigDecimal(Float.toString(var0));
         case FLOAT:
         case DOUBLE:
            return new Double(new Float(var0).doubleValue());
         case REAL:
            return new Float(var0);
         case BINARY:
            return convertIntToBytes(Float.floatToRawIntBits(var0), 4);
         default:
            return Float.toString(var0);
      }
   }

   static final byte[] convertLongToBytes(long var0) {
      byte[] var2 = new byte[8];
      int var3 = 8;

      while (var3-- > 0) {
         var2[var3] = (byte)(var0 & 255L);
         var0 >>= 8;
      }

      return var2;
   }

   static final Object convertDoubleToObject(double var0, JDBCType var2, StreamType var3) {
      switch (var2) {
         case INTEGER:
            return new Integer((int)var0);
         case SMALLINT:
         case TINYINT:
            return new Short((short)var0);
         case BIT:
         case BOOLEAN:
            return new Boolean(0 != Double.compare(0.0, var0));
         case BIGINT:
            return new Long((long)var0);
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return new BigDecimal(Double.toString(var0));
         case FLOAT:
         case DOUBLE:
            return new Double(var0);
         case REAL:
            return new Float(new Double(var0).floatValue());
         case BINARY:
            return convertLongToBytes(Double.doubleToRawLongBits(var0));
         default:
            return Double.toString(var0);
      }
   }

   static final byte[] convertBigDecimalToBytes(BigDecimal var0, int var1) {
      byte[] var2;
      if (var0 == null) {
         var2 = new byte[]{(byte)var1, 0};
      } else {
         boolean var3 = var0.signum() < 0;
         if (var0.scale() < 0) {
            var0 = var0.setScale(0);
         }

         BigInteger var4 = var0.unscaledValue();
         if (var3) {
            var4 = var4.negate();
         }

         byte[] var5 = var4.toByteArray();
         var2 = new byte[var5.length + 3];
         int var6 = 0;
         var2[var6++] = (byte)var0.scale();
         var2[var6++] = (byte)(var5.length + 1);
         var2[var6++] = (byte)(var3 ? 0 : 1);

         for (int var7 = var5.length - 1; var7 >= 0; var7--) {
            var2[var6++] = var5[var7];
         }
      }

      return var2;
   }

   static final Object convertBigDecimalToObject(BigDecimal var0, JDBCType var1, StreamType var2) {
      switch (var1) {
         case INTEGER:
            return new Integer(var0.intValue());
         case SMALLINT:
         case TINYINT:
            return new Short(var0.shortValue());
         case BIT:
         case BOOLEAN:
            return new Boolean(0 != var0.compareTo(BigDecimal.valueOf(0L)));
         case BIGINT:
            return new Long(var0.longValue());
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return var0;
         case FLOAT:
         case DOUBLE:
            return new Double(var0.doubleValue());
         case REAL:
            return new Float(var0.floatValue());
         case BINARY:
            return convertBigDecimalToBytes(var0, var0.scale());
         default:
            return var0.toString();
      }
   }

   static final Object convertMoneyToObject(BigDecimal var0, JDBCType var1, StreamType var2, int var3) {
      switch (var1) {
         case INTEGER:
            return new Integer(var0.intValue());
         case SMALLINT:
         case TINYINT:
            return new Short(var0.shortValue());
         case BIT:
         case BOOLEAN:
            return new Boolean(0 != var0.compareTo(BigDecimal.valueOf(0L)));
         case BIGINT:
            return new Long(var0.longValue());
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return var0;
         case FLOAT:
         case DOUBLE:
            return new Double(var0.doubleValue());
         case REAL:
            return new Float(var0.floatValue());
         case BINARY:
            return convertToBytes(var0, var0.scale(), var3);
         default:
            return var0.toString();
      }
   }

   private static byte[] convertToBytes(BigDecimal var0, int var1, int var2) {
      boolean var3 = var0.signum() < 0;
      var0 = var0.setScale(var1, RoundingMode.DOWN);
      BigInteger var4 = var0.unscaledValue();
      byte[] var5 = var4.toByteArray();
      byte[] var6 = new byte[var2];
      if (var5.length < var2) {
         for (int var7 = 0; var7 < var2 - var5.length; var7++) {
            var6[var7] = (byte)(var3 ? -1 : 0);
         }
      }

      int var10 = var2 - var5.length;

      for (int var8 = var10; var8 < var2; var8++) {
         var6[var8] = var5[var8 - var10];
      }

      return var6;
   }

   static final Object convertBytesToObject(byte[] var0, JDBCType var1, TypeInfo var2) throws SQLServerException {
      switch (var1) {
         case BINARY:
         case VARBINARY:
         case LONGVARBINARY:
            if (SSType.BINARY == var2.getSSType() && var0.length < var2.getPrecision()) {
               byte[] var6 = new byte[var2.getPrecision()];
               System.arraycopy(var0, 0, var6, 0, var0.length);
               return var6;
            }

            return var0;
         case CHAR:
            String var3 = Util.bytesToHexString(var0, var0.length);
            if (SSType.BINARY == var2.getSSType() && var3.length() < var2.getPrecision() * 2) {
               StringBuffer var5 = new StringBuffer(var3);

               while (var5.length() < var2.getPrecision() * 2) {
                  var5.append('0');
               }

               return var5.toString();
            } else {
               return var3;
            }
         default:
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_unsupportedConversionFromTo"));
            throw new SQLServerException(var4.format(new Object[]{var2.getSSType().name(), var1}), null, 0, null);
      }
   }

   static final Object convertStringToObject(String var0, String var1, JDBCType var2, StreamType var3) throws UnsupportedEncodingException, IllegalArgumentException {
      switch (var2) {
         case INTEGER:
            return Integer.valueOf(var0.trim());
         case SMALLINT:
         case TINYINT:
            return Short.valueOf(var0.trim());
         case BIT:
         case BOOLEAN:
            String var4 = var0.trim();
            return 1 == var4.length() ? '1' == var4.charAt(0) : Boolean.valueOf(var4);
         case BIGINT:
            return Long.valueOf(var0.trim());
         case DECIMAL:
         case NUMERIC:
         case MONEY:
         case SMALLMONEY:
            return new BigDecimal(var0.trim());
         case FLOAT:
         case DOUBLE:
            return Double.valueOf(var0.trim());
         case REAL:
            return Float.valueOf(var0.trim());
         case BINARY:
            return var0.getBytes(var1);
         case VARBINARY:
         case CHAR:
         case LONGVARBINARY:
         default:
            switch (var3) {
               case CHARACTER:
                  return new StringReader(var0);
               case ASCII:
                  return new ByteArrayInputStream(var0.getBytes("US-ASCII"));
               case BINARY:
                  return new ByteArrayInputStream(var0.getBytes());
               default:
                  return var0;
            }
         case TIMESTAMP:
            return Timestamp.valueOf(var0.trim());
         case DATE:
            return Date.valueOf(getDatePart(var0.trim()));
         case TIME:
            Timestamp var5 = Timestamp.valueOf("1970-01-01 " + getTimePart(var0.trim()));
            GregorianCalendar var6 = new GregorianCalendar(Locale.US);
            var6.clear();
            var6.setTimeInMillis(var5.getTime());
            if (var5.getNanos() % 1000000 >= 500000) {
               var6.add(14, 1);
            }

            var6.set(1970, 0, 1);
            return new Time(var6.getTimeInMillis());
      }
   }

   static final Object convertStreamToObject(BaseInputStream var0, TypeInfo var1, JDBCType var2, InputStreamGetterArgs var3) throws SQLServerException {
      if (null == var0) {
         return null;
      }

      assert null != var1;
      assert null != var3;
      SSType var4 = var1.getSSType();

      try {
         switch (var2) {
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
               if (StreamType.BINARY == var3.streamType) {
                  return var0;
               } else {
                  if (JDBCType.BLOB == var2) {
                     return new SQLServerBlob(var0);
                  }

                  return var0.getBytes();
               }
            case CHAR:
            case TIMESTAMP:
            case DATE:
            case TIME:
            case VARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            default:
               if (SSType.BINARY != var4
                  && SSType.VARBINARY != var4
                  && SSType.VARBINARYMAX != var4
                  && SSType.TIMESTAMP != var4
                  && SSType.IMAGE != var4
                  && SSType.UDT != var4) {
                  if (StreamType.ASCII == var3.streamType) {
                     if (var1.supportsFastAsciiConversion()) {
                        return new AsciiFilteredInputStream(var0);
                     } else {
                        if (var3.isAdaptive) {
                           return AsciiFilteredUnicodeInputStream.MakeAsciiFilteredUnicodeInputStream(
                              var0, new BufferedReader(new InputStreamReader(var0, var1.getCharset()))
                           );
                        }

                        return new ByteArrayInputStream(new String(var0.getBytes(), var1.getCharset()).getBytes("US-ASCII"));
                     }
                  } else if (StreamType.CHARACTER != var3.streamType && StreamType.NCHARACTER != var3.streamType) {
                     return convertStringToObject(new String(var0.getBytes(), var1.getCharset()), var1.getCharset(), var2, var3.streamType);
                  } else {
                     if (var3.isAdaptive) {
                        return new BufferedReader(new InputStreamReader(var0, var1.getCharset()));
                     }

                     return new StringReader(new String(var0.getBytes(), var1.getCharset()));
                  }
               } else if (StreamType.ASCII == var3.streamType) {
                  return var0;
               } else {
                  assert StreamType.CHARACTER == var3.streamType || StreamType.NONE == var3.streamType;
                  byte[] var5 = var0.getBytes();
                  if (JDBCType.GUID == var2) {
                     return Util.readGUID(var5);
                  } else {
                     String var10 = Util.bytesToHexString(var5, var5.length);
                     if (StreamType.NONE == var3.streamType) {
                        return var10;
                     }

                     return new StringReader(var10);
                  }
               }
            case CLOB:
               return new SQLServerClob(var0, var1);
            case NCLOB:
               return new SQLServerNClob(var0, var1);
            case SQLXML:
               return new SQLServerSQLXML(var0, var3, var1);
         }
      } catch (IllegalArgumentException var7) {
         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
         throw new SQLServerException(var9.format(new Object[]{var1.getSSType(), var2}), null, 0, var7);
      } catch (UnsupportedEncodingException var8) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
         throw new SQLServerException(var6.format(new Object[]{var1.getSSType(), var2}), null, 0, var8);
      }
   }

   private static final String getDatePart(String var0) {
      int var1 = var0.indexOf(32);
      return -1 == var1 ? var0 : var0.substring(0, var1);
   }

   private static final String getTimePart(String var0) {
      int var1 = var0.indexOf(32);
      return -1 == var1 ? var0 : var0.substring(var1 + 1);
   }

   private static String fractionalSecondsString(long var0, int var2) {
      assert 0L <= var0 && var0 < 1000000000L;
      assert 0 <= var2 && var2 <= 7;
      return 0 == var2 ? "" : BigDecimal.valueOf(var0 % 1000000000L, 9).setScale(var2).toPlainString().substring(1);
   }

   static final Object convertTemporalToObject(JDBCType var0, SSType var1, Calendar var2, int var3, long var4, int var6) {
      TimeZone var7 = null != var2 ? var2.getTimeZone() : TimeZone.getDefault();
      TimeZone var8 = SSType.DATETIMEOFFSET == var1 ? UTC.timeZone : var7;
      int var9 = 0;
      GregorianCalendar var10 = new GregorianCalendar(var8, Locale.US);
      var10.setLenient(true);
      var10.clear();
      switch (var1) {
         case TIME:
            var10.set(1900, 0, 1, 0, 0, 0);
            var10.set(14, (int)(var4 / 1000000L));
            var9 = (int)(var4 % 1000000000L);
            break;
         case DATE:
         case DATETIME2:
         case DATETIMEOFFSET:
            if (var3 >= GregorianChange.DAYS_SINCE_BASE_DATE_HINT) {
               var10.set(1, 0, 1 + var3 + GregorianChange.EXTRA_DAYS_TO_BE_ADDED, 0, 0, 0);
               var10.set(14, (int)(var4 / 1000000L));
            } else {
               var10.setGregorianChange(GregorianChange.PURE_CHANGE_DATE);
               var10.set(1, 0, 1 + var3, 0, 0, 0);
               var10.set(14, (int)(var4 / 1000000L));
               int var11 = var10.get(1);
               int var12 = var10.get(2);
               int var13 = var10.get(5);
               int var14 = var10.get(11);
               int var15 = var10.get(12);
               int var16 = var10.get(13);
               int var17 = var10.get(14);
               var10.setGregorianChange(GregorianChange.STANDARD_CHANGE_DATE);
               var10.set(var11, var12, var13, var14, var15, var16);
               var10.set(14, var17);
            }

            if (SSType.DATETIMEOFFSET == var1 && !var8.hasSameRules(var7)) {
               GregorianCalendar var19 = new GregorianCalendar(var7, Locale.US);
               var19.clear();
               var19.setTimeInMillis(var10.getTimeInMillis());
               var10 = var19;
            }

            var9 = (int)(var4 % 1000000000L);
            break;
         case DATETIME:
            var10.set(1900, 0, 1 + var3, 0, 0, 0);
            var10.set(14, (int)var4);
            var9 = (int)(var4 * 1000000L % 1000000000L);
            break;
         default:
            throw new AssertionError("Unexpected SSType: " + var1);
      }

      switch (var0.category) {
         case BINARY:
            switch (var1) {
               case TIME:
                  if (var9 % 1000000 >= 500000) {
                     var10.add(14, 1);
                  }

                  var10.set(1970, 0, 1);
                  return new Time(var10.getTimeInMillis());
               case DATE:
                  var10.set(11, 0);
                  var10.set(12, 0);
                  var10.set(13, 0);
                  var10.set(14, 0);
                  return new Date(var10.getTimeInMillis());
               case DATETIME2:
               case DATETIME:
                  Timestamp var24 = new Timestamp(var10.getTimeInMillis());
                  var24.setNanos(var9);
                  return var24;
               case DATETIMEOFFSET:
                  assert SSType.DATETIMEOFFSET == var1;
                  int var23 = var2.get(15);
                  assert 0 == var23 % 60000;
                  Timestamp var27 = new Timestamp(var10.getTimeInMillis());
                  var27.setNanos(var9);
                  return DateTimeOffset.valueOf(var27, var23 / 60000);
               default:
                  throw new AssertionError("Unexpected SSType: " + var1);
            }
         case DATE:
            var10.set(11, 0);
            var10.set(12, 0);
            var10.set(13, 0);
            var10.set(14, 0);
            return new Date(var10.getTimeInMillis());
         case TIME:
            if (var9 % 1000000 >= 500000) {
               var10.add(14, 1);
            }

            var10.set(1970, 0, 1);
            return new Time(var10.getTimeInMillis());
         case TIMESTAMP:
            Timestamp var22 = new Timestamp(var10.getTimeInMillis());
            var22.setNanos(var9);
            return var22;
         case DATETIMEOFFSET:
            assert SSType.DATETIMEOFFSET == var1;
            int var21 = var2.get(15);
            assert 0 == var21 % 60000;
            Timestamp var26 = new Timestamp(var10.getTimeInMillis());
            var26.setNanos(var9);
            return DateTimeOffset.valueOf(var26, var21 / 60000);
         case CHARACTER:
            switch (var1) {
               case TIME:
                  return String.format(Locale.US, "%1$tT%2$s", new Object[]{var10, fractionalSecondsString(var9, var6)});
               case DATE:
                  return String.format(Locale.US, "%1$tF", new Object[]{var10});
               case DATETIME2:
                  return String.format(Locale.US, "%1$tF %1$tT%2$s", new Object[]{var10, fractionalSecondsString(var9, var6)});
               case DATETIMEOFFSET:
                  int var20 = var2.get(15);
                  assert 0 == var20 % 60000;
                  int var25 = Math.abs(var20 / 60000);
                  return String.format(
                     Locale.US,
                     "%1$tF %1$tT%2$s %3$c%4$02d:%5$02d",
                     new Object[]{var10, fractionalSecondsString(var9, var6), Character.valueOf((char)(var20 >= 0 ? '+' : '-')), var25 / 60, var25 % 60}
                  );
               case DATETIME:
                  return new Timestamp(var10.getTimeInMillis()).toString();
               default:
                  throw new AssertionError("Unexpected SSType: " + var1);
            }
         default:
            throw new AssertionError("Unexpected JDBCType: " + var0);
      }
   }

   static int daysSinceBaseDate(int var0, int var1, int var2) {
      assert var0 >= 1;
      assert var2 >= 1;
      assert var1 >= 1;
      return var1 - 1 + (var0 - var2) * 365 + leapDaysBeforeYear(var0) - leapDaysBeforeYear(var2);
   }

   private static int leapDaysBeforeYear(int var0) {
      assert var0 >= 1;
      return (var0 - 1) / 4 - (var0 - 1) / 100 + (var0 - 1) / 400;
   }

   static final boolean exceedsMaxRPCDecimalPrecisionOrScale(BigDecimal var0) {
      if (null == var0) {
         return false;
      }

      if (var0.scale() > 38) {
         return true;
      }

      BigInteger var1 = var0.scale() < 0 ? var0.setScale(0).unscaledValue() : var0.unscaledValue();
      if (var0.signum() < 0) {
         var1 = var1.negate();
      }

      return var1.compareTo(maxRPCDecimalValue) > 0;
   }

   static String convertReaderToString(Reader var0, int var1) throws SQLServerException {
      assert -1 == var1 || var1 >= 0;
      if (null == var0) {
         return null;
      }

      if (0 == var1) {
         return "";
      }

      try {
         StringBuilder var2 = new StringBuilder(-1 != var1 ? var1 : 4000);
         char[] var8 = new char[-1 != var1 && var1 < 4000 ? var1 : 4000];

         int var9;
         while ((var9 = var0.read(var8, 0, var8.length)) > 0) {
            if (var9 > var8.length) {
               MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var6 = new Object[]{SQLServerException.getErrString("R_streamReadReturnedInvalidValue")};
               SQLServerException.makeFromDriverError(null, null, var5.format(var6), "", true);
            }

            var2.append(var8, 0, var9);
         }

         return var2.toString();
      } catch (IOException var7) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
         Object[] var4 = new Object[]{var7.toString()};
         SQLServerException.makeFromDriverError(null, null, var3.format(var4), "", true);
         return null;
      }
   }
}
