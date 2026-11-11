package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

final class Util {
   static final String SYSTEM_SPEC_VERSION = System.getProperty("java.specification.version");
   static final char[] hexChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
   static final String WSIDNotAvailable = "";
   static final String ActivityIdTraceProperty = "com.microsoft.sqlserver.jdbc.traceactivity";
   static final String SYSTEM_JRE = System.getProperty("java.vendor") + " " + System.getProperty("java.version");

   static boolean isIBM() {
      return SYSTEM_JRE.startsWith("IBM");
   }

   static final Boolean isCharType(int var0) {
      switch (var0) {
         case -16:
         case -15:
         case -9:
         case -1:
         case 1:
         case 12:
            return true;
         default:
            return false;
      }
   }

   static final Boolean isCharType(SSType var0) {
      switch (var0) {
         case CHAR:
         case NCHAR:
         case VARCHAR:
         case NVARCHAR:
         case VARCHARMAX:
         case NVARCHARMAX:
            return true;
         default:
            return false;
      }
   }

   static final Boolean isBinaryType(SSType var0) {
      switch (var0) {
         case BINARY:
         case VARBINARY:
         case VARBINARYMAX:
         case IMAGE:
            return true;
         default:
            return false;
      }
   }

   static final Boolean isBinaryType(int var0) {
      switch (var0) {
         case -4:
         case -3:
         case -2:
            return true;
         default:
            return false;
      }
   }

   static short readShort(byte[] var0, int var1) {
      return (short)(var0[var1] & 0xFF | (var0[var1 + 1] & 255) << 8);
   }

   static int readUnsignedShort(byte[] var0, int var1) {
      return var0[var1] & 0xFF | (var0[var1 + 1] & 0xFF) << 8;
   }

   static int readUnsignedShortBigEndian(byte[] var0, int var1) {
      return (var0[var1] & 0xFF) << 8 | var0[var1 + 1] & 0xFF;
   }

   static void writeShort(short var0, byte[] var1, int var2) {
      var1[var2 + 0] = (byte)(var0 >> 0 & 0xFF);
      var1[var2 + 1] = (byte)(var0 >> 8 & 0xFF);
   }

   static void writeShortBigEndian(short var0, byte[] var1, int var2) {
      var1[var2 + 0] = (byte)(var0 >> 8 & 0xFF);
      var1[var2 + 1] = (byte)(var0 >> 0 & 0xFF);
   }

   static int readInt(byte[] var0, int var1) {
      int var2 = var0[var1 + 0] & 255;
      int var3 = (var0[var1 + 1] & 255) << 8;
      int var4 = (var0[var1 + 2] & 255) << 16;
      int var5 = (var0[var1 + 3] & 255) << 24;
      return var5 | var4 | var3 | var2;
   }

   static int readIntBigEndian(byte[] var0, int var1) {
      return (var0[var1 + 3] & 0xFF) << 0 | (var0[var1 + 2] & 0xFF) << 8 | (var0[var1 + 1] & 0xFF) << 16 | (var0[var1 + 0] & 0xFF) << 24;
   }

   static void writeInt(int var0, byte[] var1, int var2) {
      var1[var2 + 0] = (byte)(var0 >> 0 & 0xFF);
      var1[var2 + 1] = (byte)(var0 >> 8 & 0xFF);
      var1[var2 + 2] = (byte)(var0 >> 16 & 0xFF);
      var1[var2 + 3] = (byte)(var0 >> 24 & 0xFF);
   }

   static void writeIntBigEndian(int var0, byte[] var1, int var2) {
      var1[var2 + 0] = (byte)(var0 >> 24 & 0xFF);
      var1[var2 + 1] = (byte)(var0 >> 16 & 0xFF);
      var1[var2 + 2] = (byte)(var0 >> 8 & 0xFF);
      var1[var2 + 3] = (byte)(var0 >> 0 & 0xFF);
   }

   static void writeLongBigEndian(long var0, byte[] var2, int var3) {
      var2[var3 + 0] = (byte)(var0 >> 56 & 255L);
      var2[var3 + 1] = (byte)(var0 >> 48 & 255L);
      var2[var3 + 2] = (byte)(var0 >> 40 & 255L);
      var2[var3 + 3] = (byte)(var0 >> 32 & 255L);
      var2[var3 + 4] = (byte)(var0 >> 24 & 255L);
      var2[var3 + 5] = (byte)(var0 >> 16 & 255L);
      var2[var3 + 6] = (byte)(var0 >> 8 & 255L);
      var2[var3 + 7] = (byte)(var0 >> 0 & 255L);
   }

   static BigDecimal readBigDecimal(byte[] var0, int var1, int var2) {
      int var3 = 0 == var0[0] ? -1 : 1;
      byte[] var4 = new byte[var1 - 1];

      for (int var5 = 1; var5 <= var4.length; var5++) {
         var4[var4.length - var5] = var0[var5];
      }

      return new BigDecimal(new BigInteger(var3, var4), var2);
   }

   static long readLong(byte[] var0, int var1) {
      long var2 = 0L;

      for (int var4 = 7; var4 > 0; var4--) {
         var2 += var0[var1 + var4] & 0xFF;
         var2 <<= 8;
      }

      return var2 + (var0[var1] & 0xFF);
   }

   static Properties parseUrl(String var0, Logger var1) throws SQLServerException {
      Properties var2 = new Properties();
      String var3 = var0;
      String var4 = "jdbc:sqlserver://";
      String var5 = "";
      String var6 = "";
      String var7 = "";
      if (!var3.startsWith(var4)) {
         return null;
      }

      var3 = var3.substring(var4.length());
      boolean var8 = false;
      byte var17 = 0;

      for (int var32 = 0; var32 < var3.length(); var32++) {
         char var18 = var3.charAt(var32);
         switch (var17) {
            case 0:
               if (var18 == ';') {
                  var17 = 7;
               } else {
                  var5 = var5 + var18;
                  var17 = 1;
               }
               break;
            case 1:
               if (var18 != ';' && var18 != ':' && var18 != '\\') {
                  var5 = var5 + var18;
               } else {
                  var5 = var5.trim();
                  if (var5.length() > 0) {
                     var2.put(SQLServerDriverStringProperty.SERVER_NAME.toString(), var5);
                     if (var1.isLoggable(Level.FINE)) {
                        var1.fine("Property:serverName Value:" + var5);
                     }
                  }

                  var5 = "";
                  if (var18 == ';') {
                     var17 = 7;
                  } else if (var18 == ':') {
                     var17 = 2;
                  } else {
                     var17 = 3;
                  }
               }
               break;
            case 2:
               if (var18 == ';') {
                  var5 = var5.trim();
                  if (var1.isLoggable(Level.FINE)) {
                     var1.fine("Property:portNumber Value:" + var5);
                  }

                  var2.put(SQLServerDriverIntProperty.PORT_NUMBER.toString(), var5);
                  var5 = "";
                  var17 = 7;
               } else {
                  var5 = var5 + var18;
               }
               break;
            case 3:
               if (var18 != ';' && var18 != ':') {
                  var5 = var5 + var18;
               } else {
                  var5 = var5.trim();
                  if (var1.isLoggable(Level.FINE)) {
                     var1.fine("Property:instanceName Value:" + var5);
                  }

                  var2.put(SQLServerDriverStringProperty.INSTANCE_NAME.toString(), var5.toLowerCase(Locale.US));
                  var5 = "";
                  if (var18 == ';') {
                     var17 = 7;
                  } else {
                     var17 = 2;
                  }
               }
               break;
            case 4:
               if (var18 == '}') {
                  var6 = SQLServerDriver.getNormalizedPropertyName(var6, var1);
                  if (null != var6) {
                     if (var1.isLoggable(Level.FINE)
                        && !var6.equals(SQLServerDriverStringProperty.USER.toString())
                        && !var6.equals(SQLServerDriverStringProperty.PASSWORD.toString())) {
                        var1.fine("Property:" + var6 + " Value:" + var7);
                     }

                     var2.put(var6, var7);
                  }

                  var6 = "";
                  var7 = "";
                  var17 = 5;
               } else {
                  var7 = var7 + var18;
               }
               break;
            case 5:
               if (var18 == ';') {
                  var17 = 7;
               } else if (var18 != ' ') {
                  SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
               }
               break;
            case 6:
               if (var18 == ';') {
                  var7 = var7.trim();
                  var6 = SQLServerDriver.getNormalizedPropertyName(var6, var1);
                  if (null != var6) {
                     if (var1.isLoggable(Level.FINE)
                        && !var6.equals(SQLServerDriverStringProperty.USER.toString())
                        && !var6.equals(SQLServerDriverStringProperty.PASSWORD.toString())) {
                        var1.fine("Property:" + var6 + " Value:" + var7);
                     }

                     var2.put(var6, var7);
                  }

                  var6 = "";
                  var7 = "";
                  var17 = 7;
               } else if (var18 == '{') {
                  var17 = 4;
                  var7 = var7.trim();
                  if (var7.length() > 0) {
                     SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
                  }
               } else {
                  var7 = var7 + var18;
               }
               break;
            case 7:
               if (var18 == '=') {
                  var6 = var6.trim();
                  if (var6.length() <= 0) {
                     SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
                  }

                  var17 = 6;
               } else if (var18 == ';') {
                  var6 = var6.trim();
                  if (var6.length() > 0) {
                     SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
                  }
               } else {
                  var6 = var6 + var18;
               }
               break;
            default:
               assert false : "parseURL: Invalid state " + var17;
         }
      }

      switch (var17) {
         case 0:
         case 5:
            break;
         case 1:
            var5 = var5.trim();
            if (var5.length() > 0) {
               if (var1.isLoggable(Level.FINE)) {
                  var1.fine("Property:serverName Value:" + var5);
               }

               var2.put(SQLServerDriverStringProperty.SERVER_NAME.toString(), var5);
            }
            break;
         case 2:
            var5 = var5.trim();
            if (var1.isLoggable(Level.FINE)) {
               var1.fine("Property:portNumber Value:" + var5);
            }

            var2.put(SQLServerDriverIntProperty.PORT_NUMBER.toString(), var5);
            break;
         case 3:
            var5 = var5.trim();
            if (var1.isLoggable(Level.FINE)) {
               var1.fine("Property:instanceName Value:" + var5);
            }

            var2.put(SQLServerDriverStringProperty.INSTANCE_NAME.toString(), var5);
            break;
         case 4:
         default:
            SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
            break;
         case 6:
            var7 = var7.trim();
            var6 = SQLServerDriver.getNormalizedPropertyName(var6, var1);
            if (null != var6) {
               if (var1.isLoggable(Level.FINE)
                  && !var6.equals(SQLServerDriverStringProperty.USER.toString())
                  && !var6.equals(SQLServerDriverStringProperty.PASSWORD.toString())) {
                  var1.fine("Property:" + var6 + " Value:" + var7);
               }

               var2.put(var6, var7);
            }
            break;
         case 7:
            var6 = var6.trim();
            if (var6.length() > 0) {
               SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
            }
      }

      return var2;
   }

   static String escapeSQLId(String var0) {
      StringBuilder var2 = new StringBuilder(var0.length() + 2);
      var2.append('[');

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var1 = var0.charAt(var3);
         if (']' == var1) {
            var2.append("]]");
         } else {
            var2.append(var1);
         }
      }

      var2.append(']');
      return var2.toString();
   }

   static void checkDuplicateColumnName(String var0, Map<Integer, ?> var1) throws SQLServerException {
      if (var1.get(0) instanceof SQLServerMetaData) {
         for (Entry var3 : var1.entrySet()) {
            SQLServerMetaData var4 = (SQLServerMetaData)var3.getValue();
            if (var4.columnName.equals(var0)) {
               MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_TVPDuplicateColumnName"));
               Object[] var6 = new Object[]{var0};
               throw new SQLServerException(null, var5.format(var6), null, 0, false);
            }
         }
      } else if (var1.get(0) instanceof SQLServerDataColumn) {
         for (Entry var8 : var1.entrySet()) {
            SQLServerDataColumn var9 = (SQLServerDataColumn)var8.getValue();
            if (var9.columnName.equals(var0)) {
               MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_TVPDuplicateColumnName"));
               Object[] var11 = new Object[]{var0};
               throw new SQLServerException(null, var10.format(var11), null, 0, false);
            }
         }
      }
   }

   static String readUnicodeString(byte[] var0, int var1, int var2, SQLServerConnection var3) throws SQLServerException {
      try {
         return new String(var0, var1, var2, Encoding.UNICODE.charsetName());
      } catch (UnsupportedEncodingException var8) {
         String var10 = SQLServerException.checkAndAppendClientConnId(SQLServerException.getErrString("R_stringReadError"), var3);
         MessageFormat var11 = new MessageFormat(var10);
         Object[] var12 = new Object[]{new Integer(var1)};
         throw new SQLServerException(null, var11.format(var12), null, 0, true);
      } catch (IndexOutOfBoundsException var9) {
         String var5 = SQLServerException.checkAndAppendClientConnId(SQLServerException.getErrString("R_stringReadError"), var3);
         MessageFormat var6 = new MessageFormat(var5);
         Object[] var7 = new Object[]{new Integer(var1)};
         throw new SQLServerException(null, var6.format(var7), null, 0, true);
      }
   }

   static String byteToHexDisplayString(byte[] var0) {
      if (null == var0) {
         return "(null)";
      }

      StringBuilder var2 = new StringBuilder(var0.length * 2 + 2);
      var2.append("0x");

      for (int var3 = 0; var3 < var0.length; var3++) {
         int var1 = var0[var3] & 255;
         var2.append(hexChars[(var1 & 240) >> 4]);
         var2.append(hexChars[var1 & 15]);
      }

      return var2.toString();
   }

   static String bytesToHexString(byte[] var0, int var1) {
      StringBuilder var2 = new StringBuilder(var1 * 2);

      for (int var3 = 0; var3 < var1; var3++) {
         int var4 = var0[var3] & 255;
         var2.append(hexChars[(var4 & 240) >> 4]);
         var2.append(hexChars[var4 & 15]);
      }

      return var2.toString();
   }

   static String lookupHostName() {
      try {
         InetAddress var0 = InetAddress.getLocalHost();
         if (null != var0) {
            String var1 = var0.getHostName();
            if (null != var1 && var1.length() > 0) {
               return var1;
            }

            var1 = var0.getHostAddress();
            if (null != var1 && var1.length() > 0) {
               return var1;
            }
         }

         return "";
      } catch (UnknownHostException var2) {
         return "";
      }
   }

   static final byte[] asGuidByteArray(UUID var0) {
      long var1 = var0.getMostSignificantBits();
      long var3 = var0.getLeastSignificantBits();
      byte[] var5 = new byte[16];
      writeLongBigEndian(var1, var5, 0);
      writeLongBigEndian(var3, var5, 8);
      byte var6 = var5[0];
      var5[0] = var5[3];
      var5[3] = var6;
      var6 = var5[1];
      var5[1] = var5[2];
      var5[2] = var6;
      var6 = var5[4];
      var5[4] = var5[5];
      var5[5] = var6;
      var6 = var5[6];
      var5[6] = var5[7];
      var5[7] = var6;
      return var5;
   }

   static final String readGUID(byte[] var0) throws SQLServerException {
      String var1 = "NNNNNNNN-NNNN-NNNN-NNNN-NNNNNNNNNNNN";
      byte[] var2 = var0;
      StringBuilder var3 = new StringBuilder(var1.length());

      for (int var4 = 0; var4 < 4; var4++) {
         var3.append(hexChars[(var2[3 - var4] & 240) >> 4]);
         var3.append(hexChars[var2[3 - var4] & 15]);
      }

      var3.append('-');

      for (int var5 = 0; var5 < 2; var5++) {
         var3.append(hexChars[(var2[5 - var5] & 240) >> 4]);
         var3.append(hexChars[var2[5 - var5] & 15]);
      }

      var3.append('-');

      for (int var6 = 0; var6 < 2; var6++) {
         var3.append(hexChars[(var2[7 - var6] & 240) >> 4]);
         var3.append(hexChars[var2[7 - var6] & 15]);
      }

      var3.append('-');

      for (int var7 = 0; var7 < 2; var7++) {
         var3.append(hexChars[(var2[8 + var7] & 240) >> 4]);
         var3.append(hexChars[var2[8 + var7] & 15]);
      }

      var3.append('-');

      for (int var8 = 0; var8 < 6; var8++) {
         var3.append(hexChars[(var2[10 + var8] & 240) >> 4]);
         var3.append(hexChars[var2[10 + var8] & 15]);
      }

      return var3.toString();
   }

   static boolean IsActivityTraceOn() {
      LogManager var0 = LogManager.getLogManager();
      String var1 = var0.getProperty("com.microsoft.sqlserver.jdbc.traceactivity");
      return null != var1 && var1.equalsIgnoreCase("on");
   }

   static boolean shouldHonorAEForRead(SQLServerStatementColumnEncryptionSetting var0, SQLServerConnection var1) {
      switch (var0) {
         case Disabled:
            return false;
         case Enabled:
            return true;
         case ResultSetOnly:
            return true;
         default:
            assert SQLServerStatementColumnEncryptionSetting.UseConnectionSetting == var0 : "Unexpected value for command level override";
            return var1 != null && var1.isColumnEncryptionSettingEnabled();
      }
   }

   static boolean shouldHonorAEForParameters(SQLServerStatementColumnEncryptionSetting var0, SQLServerConnection var1) {
      switch (var0) {
         case Disabled:
            return false;
         case Enabled:
            return true;
         case ResultSetOnly:
            return false;
         default:
            assert SQLServerStatementColumnEncryptionSetting.UseConnectionSetting == var0 : "Unexpected value for command level override";
            return var1 != null && var1.isColumnEncryptionSettingEnabled();
      }
   }

   static void validateMoneyRange(BigDecimal var0, JDBCType var1) throws SQLServerException {
      if (null != var0) {
         switch (var1) {
            case MONEY:
               if (1 != var0.compareTo(SSType.MAX_VALUE_MONEY) && -1 != var0.compareTo(SSType.MIN_VALUE_MONEY)) {
                  return;
               }
               break;
            case SMALLMONEY:
               if (1 != var0.compareTo(SSType.MAX_VALUE_SMALLMONEY) && -1 != var0.compareTo(SSType.MIN_VALUE_SMALLMONEY)) {
                  return;
               }
         }

         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
         Object[] var3 = new Object[]{var1};
         throw new SQLServerException(var2.format(var3), null);
      }
   }

   static int getValueLengthBaseOnJavaType(Object var0, JavaType var1, Integer var2, Integer var3, JDBCType var4) {
      switch (var1) {
         case OBJECT:
            switch (var4) {
               case DECIMAL:
               case NUMERIC:
                  var1 = JavaType.BIGDECIMAL;
                  break;
               case TIME:
                  var1 = JavaType.TIME;
                  break;
               case TIMESTAMP:
                  var1 = JavaType.TIMESTAMP;
                  break;
               case DATETIMEOFFSET:
                  var1 = JavaType.DATETIMEOFFSET;
            }
         default:
            switch (var1) {
               case STRING:
                  if (JDBCType.GUID == var4) {
                     String var9 = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";
                     return null == var0 ? 0 : var9.length();
                  } else {
                     if (JDBCType.TIMESTAMP != var4 && JDBCType.TIME != var4 && JDBCType.DATETIMEOFFSET != var4) {
                        return null == var0 ? 0 : ((String)var0).length();
                     }

                     return null == var3 ? 7 : var3;
                  }
               case BYTEARRAY:
                  return null == var0 ? 0 : ((byte[])var0).length;
               case BIGDECIMAL:
                  int var5 = -1;
                  if (null == var2) {
                     if (null == var0) {
                        var5 = 0;
                     } else if (0 == ((BigDecimal)var0).intValue()) {
                        String var6 = "" + (BigDecimal)var0;
                        var6 = var6.replaceAll("\\.", "");
                        var6 = var6.replaceAll("\\-", "");
                        var5 = var6.length();
                     } else if (("" + (BigDecimal)var0).contains("E")) {
                        DecimalFormat var12 = new DecimalFormat("###.#####");
                        String var7 = var12.format((BigDecimal)var0);
                        var7 = var7.replaceAll("\\.", "");
                        var7 = var7.replaceAll("\\-", "");
                        var5 = var7.length();
                     } else {
                        var5 = ((BigDecimal)var0).precision();
                     }
                  } else {
                     var5 = var2;
                  }

                  return var5;
               case TIMESTAMP:
               case TIME:
               case DATETIMEOFFSET:
                  return null == var3 ? 7 : var3;
               case READER:
                  return null == var0 ? 0 : 1073741823;
               case CLOB:
                  return null == var0 ? 0 : 2147483646;
               case NCLOB:
                  return null == var0 ? 0 : 1073741823;
               default:
                  return 0;
            }
      }
   }

   static synchronized boolean checkIfNeedNewAccessToken(SQLServerConnection var0) {
      Date var1 = var0.getAuthenticationResult().getExpiresOnDate();
      Date var2 = new Date();
      if (var1.getTime() - var2.getTime() < 2700000L) {
         if (var1.getTime() - var2.getTime() < 600000L) {
            return true;
         }

         if (var0.attemptRefreshTokenLocked) {
            return false;
         }

         var0.attemptRefreshTokenLocked = true;
         return true;
      } else {
         return false;
      }
   }
}
