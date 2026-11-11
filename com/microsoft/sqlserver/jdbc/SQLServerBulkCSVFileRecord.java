package com.microsoft.sqlserver.jdbc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class SQLServerBulkCSVFileRecord implements ISQLServerBulkRecord, AutoCloseable {
   private BufferedReader fileReader;
   private InputStreamReader sr;
   private FileInputStream fis;
   private Map<Integer, SQLServerBulkCSVFileRecord.ColumnMetadata> columnMetadata;
   private String currentLine = null;
   private final String delimiter;
   private String[] columnNames = null;
   private DateTimeFormatter dateTimeFormatter = null;
   private DateTimeFormatter timeFormatter = null;
   private static final String loggerClassName = "com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord";
   private static final Logger loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord");

   public SQLServerBulkCSVFileRecord(String var1, String var2, String var3, boolean var4) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "SQLServerBulkCSVFileRecord", new Object[]{var1, var2, var3, var4});
      if (null == var1) {
         this.throwInvalidArgument("fileToParse");
      } else if (null == var3) {
         this.throwInvalidArgument("delimiter");
      }

      this.delimiter = var3;

      try {
         this.fis = new FileInputStream(var1);
         if (null != var2 && 0 != var2.length()) {
            this.sr = new InputStreamReader(this.fis, var2);
         } else {
            this.sr = new InputStreamReader(this.fis);
         }

         this.fileReader = new BufferedReader(this.sr);
         if (var4) {
            this.currentLine = this.fileReader.readLine();
            if (null != this.currentLine) {
               this.columnNames = this.currentLine.split(var3, -1);
            }
         }
      } catch (UnsupportedEncodingException var7) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         throw new SQLServerException(var6.format(new Object[]{var2}), null, 0, null);
      } catch (Exception var8) {
         throw new SQLServerException(null, var8.getMessage(), null, 0, false);
      }

      this.columnMetadata = new HashMap();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "SQLServerBulkCSVFileRecord");
   }

   public SQLServerBulkCSVFileRecord(String var1, String var2, boolean var3) throws SQLServerException {
      this(var1, var2, ",", var3);
   }

   public SQLServerBulkCSVFileRecord(String var1, boolean var2) throws SQLServerException {
      this(var1, null, ",", var2);
   }

   public void addColumnMetadata(int var1, String var2, int var3, int var4, int var5, DateTimeFormatter var6) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      this.addColumnMetadataInternal(var1, var2, var3, var4, var5, var6);
   }

   public void addColumnMetadata(int var1, String var2, int var3, int var4, int var5) throws SQLServerException {
      this.addColumnMetadataInternal(var1, var2, var3, var4, var5, null);
   }

   void addColumnMetadataInternal(int var1, String var2, int var3, int var4, int var5, DateTimeFormatter var6) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "addColumnMetadata", new Object[]{var1, var2, var3, var4, var5});
      String var7 = "";
      if (0 >= var1) {
         MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_invalidColumnOrdinal"));
         Object[] var11 = new Object[]{var1};
         throw new SQLServerException(var10.format(var11), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
      }

      if (null != var2) {
         var7 = var2.trim();
      } else if (this.columnNames != null && this.columnNames.length >= var1) {
         var7 = this.columnNames[var1 - 1];
      }

      if (this.columnNames != null && var1 > this.columnNames.length) {
         MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
         Object[] var9 = new Object[]{var1};
         throw new SQLServerException(var8.format(var9), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
      }

      this.checkDuplicateColumnName(var1, var2);
      switch (var3) {
         case -155:
         case 91:
         case 92:
         case 93:
            this.columnMetadata.put(var1, new SQLServerBulkCSVFileRecord.ColumnMetadata(var7, var3, 50, var5, var6));
            break;
         case 6:
            this.columnMetadata.put(var1, new SQLServerBulkCSVFileRecord.ColumnMetadata(var7, 8, var4, var5, var6));
            break;
         case 16:
            this.columnMetadata.put(var1, new SQLServerBulkCSVFileRecord.ColumnMetadata(var7, -7, var4, var5, var6));
            break;
         case 2009:
            this.columnMetadata.put(var1, new SQLServerBulkCSVFileRecord.ColumnMetadata(var7, -16, var4, var5, var6));
            break;
         default:
            this.columnMetadata.put(var1, new SQLServerBulkCSVFileRecord.ColumnMetadata(var7, var3, var4, var5, var6));
      }

      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "addColumnMetadata");
   }

   public void setTimestampWithTimezoneFormat(String var1) {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimestampWithTimezoneFormat", var1);
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(var1);
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimestampWithTimezoneFormat");
   }

   public void setTimestampWithTimezoneFormat(DateTimeFormatter var1) {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimestampWithTimezoneFormat", new Object[]{var1});
      this.dateTimeFormatter = var1;
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimestampWithTimezoneFormat");
   }

   public void setTimeWithTimezoneFormat(String var1) {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimeWithTimezoneFormat", var1);
      this.timeFormatter = DateTimeFormatter.ofPattern(var1);
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimeWithTimezoneFormat");
   }

   public void setTimeWithTimezoneFormat(DateTimeFormatter var1) {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimeWithTimezoneFormat", new Object[]{var1});
      this.timeFormatter = var1;
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "setTimeWithTimezoneFormat");
   }

   public void close() throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "close");
      if (this.fileReader != null) {
         try {
            this.fileReader.close();
         } catch (Exception var4) {
         }
      }

      if (this.sr != null) {
         try {
            this.sr.close();
         } catch (Exception var3) {
         }
      }

      if (this.fis != null) {
         try {
            this.fis.close();
         } catch (Exception var2) {
         }
      }

      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord", "close");
   }

   public DateTimeFormatter getColumnDateTimeFormatter(int var1) {
      DriverJDBCVersion.checkSupportsJDBC42();
      return ((SQLServerBulkCSVFileRecord.ColumnMetadata)this.columnMetadata.get(var1)).dateTimeFormatter;
   }

   public Set<Integer> getColumnOrdinals() {
      return this.columnMetadata.keySet();
   }

   public String getColumnName(int var1) {
      return ((SQLServerBulkCSVFileRecord.ColumnMetadata)this.columnMetadata.get(var1)).columnName;
   }

   public int getColumnType(int var1) {
      return ((SQLServerBulkCSVFileRecord.ColumnMetadata)this.columnMetadata.get(var1)).columnType;
   }

   public int getPrecision(int var1) {
      return ((SQLServerBulkCSVFileRecord.ColumnMetadata)this.columnMetadata.get(var1)).precision;
   }

   public int getScale(int var1) {
      return ((SQLServerBulkCSVFileRecord.ColumnMetadata)this.columnMetadata.get(var1)).scale;
   }

   public boolean isAutoIncrement(int var1) {
      return false;
   }

   public Object[] getRowData() throws SQLServerException {
      if (null == this.currentLine) {
         return null;
      }

      String[] var1 = this.currentLine.split(this.delimiter, -1);
      Object[] var2 = new Object[var1.length];

      for (Entry var4 : this.columnMetadata.entrySet()) {
         SQLServerBulkCSVFileRecord.ColumnMetadata var5 = (SQLServerBulkCSVFileRecord.ColumnMetadata)var4.getValue();
         if (var1.length < (Integer)var4.getKey() - 1) {
            MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
            Object[] var27 = new Object[]{var4.getKey()};
            throw new SQLServerException(var23.format(var27), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
         }

         if (this.columnNames != null && this.columnNames.length > var1.length) {
            MessageFormat var22 = new MessageFormat(SQLServerException.getErrString("R_BulkCSVDataSchemaMismatch"));
            Object[] var26 = new Object[0];
            throw new SQLServerException(var22.format(var26), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
         }

         try {
            if (0 == var1[var4.getKey() - 1].length()) {
               var2[var4.getKey() - 1] = null;
            } else {
               switch (var5.columnType) {
                  case -16:
                  case -15:
                  case -9:
                  case -1:
                  case 1:
                  case 12:
                  case 91:
                  case 2005:
                  default:
                     var2[var4.getKey() - 1] = var1[var4.getKey() - 1];
                     break;
                  case -7:
                     try {
                        var2[var4.getKey() - 1] = 0.0 == Double.parseDouble(var1[var4.getKey() - 1]) ? Boolean.FALSE : Boolean.TRUE;
                     } catch (NumberFormatException var10) {
                        var2[var4.getKey() - 1] = Boolean.parseBoolean(var1[var4.getKey() - 1]);
                     }
                     break;
                  case -6:
                  case 5:
                     DecimalFormat var21 = new DecimalFormat("#");
                     String var25 = var21.format(Double.parseDouble(var1[var4.getKey() - 1]));
                     var2[var4.getKey() - 1] = Short.valueOf(var25);
                     break;
                  case -5:
                     BigDecimal var20 = new BigDecimal(var1[var4.getKey() - 1].trim());

                     try {
                        var2[var4.getKey() - 1] = var20.setScale(0, 1).longValueExact();
                        break;
                     } catch (ArithmeticException var11) {
                        String var28 = "'" + var1[var4.getKey() - 1] + "'";
                        MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
                        throw new SQLServerException(var9.format(new Object[]{var28, JDBCType.of(var5.columnType)}), null, 0, null);
                     }
                  case -4:
                  case -3:
                  case -2:
                  case 2004:
                     String var19 = var1[var4.getKey() - 1].trim();
                     if (!var19.startsWith("0x") && !var19.startsWith("0X")) {
                        var2[var4.getKey() - 1] = var19;
                        break;
                     }

                     var2[var4.getKey() - 1] = var19.substring(2);
                     break;
                  case 0:
                     var2[var4.getKey() - 1] = null;
                     break;
                  case 2:
                  case 3:
                     BigDecimal var18 = new BigDecimal(var1[var4.getKey() - 1].trim());
                     var2[var4.getKey() - 1] = var18.setScale(var5.scale, RoundingMode.HALF_UP);
                     break;
                  case 4:
                     DecimalFormat var17 = new DecimalFormat("#");
                     String var24 = var17.format(Double.parseDouble(var1[var4.getKey() - 1]));
                     var2[var4.getKey() - 1] = Integer.valueOf(var24);
                     break;
                  case 7:
                     var2[var4.getKey() - 1] = Float.parseFloat(var1[var4.getKey() - 1]);
                     break;
                  case 8:
                     var2[var4.getKey() - 1] = Double.parseDouble(var1[var4.getKey() - 1]);
                     break;
                  case 2013:
                     DriverJDBCVersion.checkSupportsJDBC42();
                     Object var15 = null;
                     if (null != var5.dateTimeFormatter) {
                        var15 = OffsetTime.parse(var1[var4.getKey() - 1], var5.dateTimeFormatter);
                     } else if (this.timeFormatter != null) {
                        var15 = OffsetTime.parse(var1[var4.getKey() - 1], this.timeFormatter);
                     } else {
                        var15 = OffsetTime.parse(var1[var4.getKey() - 1]);
                     }

                     var2[var4.getKey() - 1] = var15;
                     break;
                  case 2014:
                     DriverJDBCVersion.checkSupportsJDBC42();
                     Object var6 = null;
                     if (null != var5.dateTimeFormatter) {
                        var6 = OffsetDateTime.parse(var1[var4.getKey() - 1], var5.dateTimeFormatter);
                     } else if (this.dateTimeFormatter != null) {
                        var6 = OffsetDateTime.parse(var1[var4.getKey() - 1], this.dateTimeFormatter);
                     } else {
                        var6 = OffsetDateTime.parse(var1[var4.getKey() - 1]);
                     }

                     var2[var4.getKey() - 1] = var6;
               }
            }
         } catch (IllegalArgumentException var12) {
            String var7 = "'" + var1[var4.getKey() - 1] + "'";
            MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
            throw new SQLServerException(var8.format(new Object[]{var7, JDBCType.of(var5.columnType)}), null, 0, null);
         } catch (ArrayIndexOutOfBoundsException var13) {
            throw new SQLServerException(SQLServerException.getErrString("R_BulkCSVDataSchemaMismatch"), null);
         }
      }

      return var2;
   }

   public boolean next() throws SQLServerException {
      try {
         this.currentLine = this.fileReader.readLine();
      } catch (IOException var2) {
         throw new SQLServerException(null, var2.getMessage(), null, 0, false);
      }

      return null != this.currentLine;
   }

   private void throwInvalidArgument(String var1) throws SQLServerException {
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
      Object[] var3 = new Object[]{var1};
      SQLServerException.makeFromDriverError(null, null, var2.format(var3), null, false);
   }

   private void checkDuplicateColumnName(int var1, String var2) throws SQLServerException {
      if (null != var2 && var2.trim().length() != 0) {
         for (Entry var4 : this.columnMetadata.entrySet()) {
            if (null != var4
               && (Integer)var4.getKey() != var1
               && null != var4.getValue()
               && var2.trim().equalsIgnoreCase(((SQLServerBulkCSVFileRecord.ColumnMetadata)var4.getValue()).columnName)) {
               throw new SQLServerException(SQLServerException.getErrString("R_BulkCSVDataDuplicateColumn"), null);
            }
         }
      }
   }

   private class ColumnMetadata {
      String columnName;
      int columnType;
      int precision;
      int scale;
      DateTimeFormatter dateTimeFormatter = null;

      ColumnMetadata(String var2, int var3, int var4, int var5, DateTimeFormatter var6) {
         this.columnName = var2;
         this.columnType = var3;
         this.precision = var4;
         this.scale = var5;
         this.dateTimeFormatter = var6;
      }
   }
}
