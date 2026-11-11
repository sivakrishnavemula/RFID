package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.RowSet;
import microsoft.sql.DateTimeOffset;

public class SQLServerBulkCopy implements AutoCloseable {
   private static final String loggerClassName = "com.microsoft.sqlserver.jdbc.SQLServerBulkCopy";
   private static final int SQL_SERVER_2016_VERSION = 13;
   private static final Logger loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy");
   private SQLServerConnection connection;
   private SQLServerBulkCopyOptions copyOptions;
   private List<SQLServerBulkCopy.ColumnMapping> columnMappings;
   private boolean ownsConnection;
   private String destinationTableName;
   private ISQLServerBulkRecord sourceBulkRecord;
   private ResultSet sourceResultSet;
   private ResultSetMetaData sourceResultSetMetaData;
   private CekTable destCekTable = null;
   private Map<Integer, SQLServerBulkCopy.BulkColumnMetaData> destColumnMetadata;
   private Map<Integer, SQLServerBulkCopy.BulkColumnMetaData> srcColumnMetadata;
   private int destColumnCount;
   private int srcColumnCount;
   private SQLServerBulkCopy.BulkTimeoutTimer timeoutTimer = null;

   public SQLServerBulkCopy(Connection var1) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "SQLServerBulkCopy", var1);
      if (null == var1 || !var1.getClass().equals(SQLServerConnection.class)) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDestConnection"), null, false);
      }

      if (var1 instanceof SQLServerConnection) {
         this.connection = (SQLServerConnection)var1;
      } else {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDestConnection"), null, false);
      }

      this.ownsConnection = false;
      this.copyOptions = new SQLServerBulkCopyOptions();
      this.initializeDefaults();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "SQLServerBulkCopy");
   }

   public SQLServerBulkCopy(String var1) throws SQLException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "SQLServerBulkCopy", "connectionUrl not traced.");
      if (var1 != null && !var1.trim().equals("")) {
         this.ownsConnection = true;
         SQLServerDriver var2 = new SQLServerDriver();
         this.connection = (SQLServerConnection)var2.connect(var1, null);
         if (null == this.connection) {
            throw new SQLServerException(null, SQLServerException.getErrString("R_invalidConnection"), null, 0, false);
         }

         this.copyOptions = new SQLServerBulkCopyOptions();
         this.initializeDefaults();
         loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "SQLServerBulkCopy");
      } else {
         throw new SQLServerException(null, SQLServerException.getErrString("R_nullConnection"), null, 0, false);
      }
   }

   public void addColumnMapping(int var1, int var2) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping", new Object[]{var1, var2});
      if (0 >= var1) {
         this.throwInvalidArgument("sourceColumn");
      } else if (0 >= var2) {
         this.throwInvalidArgument("destinationColumn");
      }

      this.columnMappings.add(new SQLServerBulkCopy.ColumnMapping(var1, var2));
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping");
   }

   public void addColumnMapping(int var1, String var2) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping", new Object[]{var1, var2});
      if (0 >= var1) {
         this.throwInvalidArgument("sourceColumn");
      } else if (null == var2 || var2.isEmpty()) {
         this.throwInvalidArgument("destinationColumn");
      }

      this.columnMappings.add(new SQLServerBulkCopy.ColumnMapping(var1, var2.trim()));
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping");
   }

   public void addColumnMapping(String var1, int var2) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping", new Object[]{var1, var2});
      if (0 >= var2) {
         this.throwInvalidArgument("destinationColumn");
      } else if (null == var1 || var1.isEmpty()) {
         this.throwInvalidArgument("sourceColumn");
      }

      this.columnMappings.add(new SQLServerBulkCopy.ColumnMapping(var1.trim(), var2));
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping");
   }

   public void addColumnMapping(String var1, String var2) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping", new Object[]{var1, var2});
      if (null == var1 || var1.isEmpty()) {
         this.throwInvalidArgument("sourceColumn");
      } else if (null == var2 || var2.isEmpty()) {
         this.throwInvalidArgument("destinationColumn");
      }

      this.columnMappings.add(new SQLServerBulkCopy.ColumnMapping(var1.trim(), var2.trim()));
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "addColumnMapping");
   }

   public void clearColumnMappings() {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "clearColumnMappings");
      this.columnMappings.clear();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "clearColumnMappings");
   }

   public void close() {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "close");
      if (this.ownsConnection) {
         try {
            this.connection.close();
         } catch (SQLException var2) {
         }
      }

      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "close");
   }

   public String getDestinationTableName() {
      return this.destinationTableName;
   }

   public void setDestinationTableName(String var1) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "setDestinationTableName", var1);
      if (null == var1 || 0 == var1.trim().length()) {
         this.throwInvalidArgument("tableName");
      }

      this.destinationTableName = var1.trim();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "setDestinationTableName");
   }

   public SQLServerBulkCopyOptions getBulkCopyOptions() {
      return this.copyOptions;
   }

   public void setBulkCopyOptions(SQLServerBulkCopyOptions var1) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "updateBulkCopyOptions", var1);
      if (!this.ownsConnection && var1.isUseInternalTransaction()) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidTransactionOption"), null, false);
      }

      this.copyOptions = var1;
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "updateBulkCopyOptions");
   }

   public void writeToServer(ResultSet var1) throws SQLServerException {
      this.writeResultSet(var1, false);
   }

   public void writeToServer(RowSet var1) throws SQLServerException {
      this.writeResultSet(var1, true);
   }

   private void writeResultSet(ResultSet var1, boolean var2) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "writeToServer");
      if (null == var1) {
         this.throwInvalidArgument("sourceData");
      }

      try {
         if (var2) {
            if (!var1.isBeforeFirst()) {
               var1.beforeFirst();
            }
         } else if (var1.isClosed()) {
            SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_resultsetClosed"), null, false);
         }
      } catch (SQLException var5) {
         throw new SQLServerException(null, var5.getMessage(), null, 0, false);
      }

      this.sourceResultSet = var1;
      this.sourceBulkRecord = null;

      try {
         this.sourceResultSetMetaData = this.sourceResultSet.getMetaData();
      } catch (SQLException var4) {
         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), var4);
      }

      this.writeToServer();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "writeToServer");
   }

   public void writeToServer(ISQLServerBulkRecord var1) throws SQLServerException {
      loggerExternal.entering("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "writeToServer");
      if (null == var1) {
         this.throwInvalidArgument("sourceData");
      }

      this.sourceBulkRecord = var1;
      this.sourceResultSet = null;
      this.writeToServer();
      loggerExternal.exiting("com.microsoft.sqlserver.jdbc.SQLServerBulkCopy", "writeToServer");
   }

   private void initializeDefaults() {
      this.columnMappings = new LinkedList();
      this.destinationTableName = null;
      this.sourceBulkRecord = null;
      this.sourceResultSet = null;
      this.sourceResultSetMetaData = null;
      this.srcColumnCount = 0;
      this.srcColumnMetadata = null;
      this.destColumnMetadata = null;
      this.destColumnCount = 0;
   }

   private void sendBulkLoadBCP() throws SQLServerException {
      final class InsertBulk extends TDSCommand {
         InsertBulk() {
            super("InsertBulk", 0);
            int var2 = SQLServerBulkCopy.this.copyOptions.getBulkCopyTimeout();
            SQLServerBulkCopy.this.timeoutTimer = var2 > 0 ? SQLServerBulkCopy.this.new BulkTimeoutTimer(var2, this) : null;
         }

         final boolean doExecute() throws SQLServerException {
            if (null != SQLServerBulkCopy.this.timeoutTimer) {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this.toString() + ": Starting bulk timer...");
               }

               SQLServerBulkCopy.this.timeoutTimer.start();
            }

            try {
               while (SQLServerBulkCopy.this.doInsertBulk(this)) {
               }
            } catch (SQLServerException var3) {
               Object var2 = var3;

               while (null != var2.getCause()) {
                  var2 = var2.getCause();
               }

               if (var2 instanceof SQLException) {
                  SQLServerBulkCopy.this.checkForTimeoutException((SQLException)var2, SQLServerBulkCopy.this.timeoutTimer);
               }

               throw var3;
            }

            if (null != SQLServerBulkCopy.this.timeoutTimer) {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this.toString() + ": Stopping bulk timer...");
               }

               SQLServerBulkCopy.this.timeoutTimer.stop();
            }

            return true;
         }
      }

      this.connection.executeCommand(new InsertBulk());
   }

   private final void writeColumnMetaDataColumnData(TDSWriter var1, int var2) throws SQLServerException {
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      SQLCollation var8 = null;
      SSType var9 = null;
      byte[] var12 = new byte[]{0, 0, 0, 0};
      var1.writeBytes(var12);
      int var13 = ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var2)).destinationColumnOrdinal;
      byte[] var14 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).flags;
      var1.writeBytes(var14);
      var3 = ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var2)).sourceColumnOrdinal;
      var5 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var3)).jdbcType;
      var6 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var3)).precision;
      var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var3)).scale;
      boolean var11 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var3)).isNullable;
      var9 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).ssType;
      var4 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).precision;
      var6 = this.validateSourcePrecision(var6, var5, var4);
      var8 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).collation;
      if (null == var8) {
         var8 = this.connection.getDatabaseCollation();
      }

      boolean var10;
      if (-15 != var5 && -9 != var5 && -16 != var5) {
         var10 = 8000 < var6 || 8000 < var4;
      } else {
         var10 = 4000 < var6 || 4000 < var4;
      }

      if (this.sourceResultSet instanceof SQLServerResultSet && this.connection.isColumnEncryptionSettingEnabled()) {
         var5 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).jdbcType;
         var6 = var4;
         var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).scale;
      }

      if ((
            null == ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).encryptionType
               || !this.copyOptions.isAllowEncryptedValueModifications()
         )
         && null == ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).cryptoMeta) {
         if ((1 == var5 || 12 == var5 || -1 == var5)
            && (SSType.BINARY == var9 || SSType.VARBINARY == var9 || SSType.VARBINARYMAX == var9 || SSType.IMAGE == var9)) {
            if (var10) {
               var1.writeByte((byte)-91);
            } else {
               var1.writeByte((byte)(SSType.BINARY == var9 ? 173 : 165));
            }

            var1.writeShort((short)var6);
         } else {
            this.writeTypeInfo(var1, var5, var7, var6, var9, var8, var10, var11, false);
         }
      } else {
         var1.writeByte((byte)-91);
         if (var10) {
            var1.writeShort((short)-1);
         } else {
            var1.writeShort((short)var6);
         }
      }

      CryptoMetadata var15 = null;
      if (null != (var15 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var13)).cryptoMeta)) {
         int var16 = var15.baseTypeInfo.getSSType().getJDBCType().asJavaSqlType();
         int var17 = var15.baseTypeInfo.getPrecision();
         if (-15 != var16 && -9 != var16 && -16 != var16) {
            var10 = 8000 < var17;
         } else {
            var10 = 4000 < var17;
         }

         var1.writeShort(var15.getOrdinal());
         var1.writeBytes(var12);
         this.writeTypeInfo(var1, var16, var15.baseTypeInfo.getScale(), var17, var15.baseTypeInfo.getSSType(), var8, var10, var11, true);
         var1.writeByte(var15.cipherAlgorithmId);
         var1.writeByte(var15.encryptionType.getValue());
         var1.writeByte(var15.normalizationRuleVersion);
      }

      int var31 = ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var2)).destinationColumnName.length();
      String var32 = ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var2)).destinationColumnName;
      byte[] var18 = new byte[2 * var31];

      for (int var19 = 0; var19 < var31; var19++) {
         char var20 = var32.charAt(var19);
         var18[2 * var19] = (byte)(var20 & 0xFF);
         var18[2 * var19 + 1] = (byte)(var20 >> '\b' & 0xFF);
      }

      var1.writeByte((byte)var31);
      var1.writeBytes(var18);
   }

   private final void writeTypeInfo(TDSWriter var1, int var2, int var3, int var4, SSType var5, SQLCollation var6, boolean var7, boolean var8, boolean var9) throws SQLServerException {
      switch (var2) {
         case -155:
            if (!var9 && null != this.sourceBulkRecord) {
               var1.writeByte(TDSType.BIGVARCHAR.byteValue());
               var1.writeShort((short)var4);
               var6.writeCollation(var1);
            } else {
               var1.writeByte(TDSType.DATETIMEOFFSETN.byteValue());
               var1.writeByte((byte)var3);
            }
            break;
         case -151:
         case -150:
         case 93:
            if (!var9 && null != this.sourceBulkRecord) {
               var1.writeByte(TDSType.BIGVARCHAR.byteValue());
               var1.writeShort((short)var4);
               var6.writeCollation(var1);
            } else {
               switch (var5) {
                  case SMALLDATETIME:
                     if (!var8) {
                        var1.writeByte(TDSType.DATETIME4.byteValue());
                     } else {
                        var1.writeByte(TDSType.DATETIMEN.byteValue());
                        var1.writeByte((byte)4);
                     }

                     return;
                  case DATETIME:
                     if (!var8) {
                        var1.writeByte(TDSType.DATETIME8.byteValue());
                     } else {
                        var1.writeByte(TDSType.DATETIMEN.byteValue());
                        var1.writeByte((byte)8);
                     }

                     return;
                  default:
                     var1.writeByte(TDSType.DATETIME2N.byteValue());
                     var1.writeByte((byte)var3);
               }
            }
            break;
         case -148:
         case -146:
         case 2:
         case 3:
            if (var9 && (SSType.MONEY == var5 || SSType.SMALLMONEY == var5)) {
               var1.writeByte(TDSType.MONEYN.byteValue());
               if (SSType.MONEY == var5) {
                  var1.writeByte((byte)8);
               } else {
                  var1.writeByte((byte)4);
               }
            } else {
               if (3 == var2) {
                  var1.writeByte(TDSType.DECIMALN.byteValue());
               } else {
                  var1.writeByte(TDSType.NUMERICN.byteValue());
               }

               var1.writeByte((byte)17);
               var1.writeByte((byte)var4);
               var1.writeByte((byte)var3);
            }
            break;
         case -145:
         case 1:
            if (var9 && SSType.GUID == var5) {
               var1.writeByte(TDSType.GUID.byteValue());
               var1.writeByte((byte)16);
            } else {
               var1.writeByte(TDSType.BIGCHAR.byteValue());
               var1.writeShort((short)var4);
               var6.writeCollation(var1);
            }
            break;
         case -16:
         case -9:
            var1.writeByte(TDSType.NVARCHAR.byteValue());
            if (var7) {
               var1.writeShort((short)-1);
            } else {
               var1.writeShort(var9 ? (short)var4 : (short)(2 * var4));
            }

            var6.writeCollation(var1);
            break;
         case -15:
            var1.writeByte(TDSType.NCHAR.byteValue());
            var1.writeShort(var9 ? (short)var4 : (short)(2 * var4));
            var6.writeCollation(var1);
            break;
         case -7:
            if (!var8) {
               var1.writeByte(TDSType.BIT1.byteValue());
            } else {
               var1.writeByte(TDSType.BITN.byteValue());
               var1.writeByte((byte)1);
            }
            break;
         case -6:
            if (!var8) {
               var1.writeByte(TDSType.INT1.byteValue());
            } else {
               var1.writeByte(TDSType.INTN.byteValue());
               var1.writeByte((byte)1);
            }
            break;
         case -5:
            if (!var8) {
               var1.writeByte(TDSType.INT8.byteValue());
            } else {
               var1.writeByte(TDSType.INTN.byteValue());
               var1.writeByte((byte)8);
            }
            break;
         case -4:
         case -3:
            var1.writeByte(TDSType.BIGVARBINARY.byteValue());
            if (var7) {
               var1.writeShort((short)-1);
            } else {
               var1.writeShort((short)var4);
            }
            break;
         case -2:
            var1.writeByte(TDSType.BIGBINARY.byteValue());
            var1.writeShort((short)var4);
            break;
         case -1:
         case 12:
            var1.writeByte(TDSType.BIGVARCHAR.byteValue());
            if (var7) {
               var1.writeShort((short)-1);
            } else {
               var1.writeShort((short)var4);
            }

            var6.writeCollation(var1);
            break;
         case 4:
            if (!var8) {
               var1.writeByte(TDSType.INT4.byteValue());
            } else {
               var1.writeByte(TDSType.INTN.byteValue());
               var1.writeByte((byte)4);
            }
            break;
         case 5:
            if (!var8) {
               var1.writeByte(TDSType.INT2.byteValue());
            } else {
               var1.writeByte(TDSType.INTN.byteValue());
               var1.writeByte((byte)2);
            }
            break;
         case 7:
            if (!var8) {
               var1.writeByte(TDSType.FLOAT4.byteValue());
            } else {
               var1.writeByte(TDSType.FLOATN.byteValue());
               var1.writeByte((byte)4);
            }
            break;
         case 8:
            if (!var8) {
               var1.writeByte(TDSType.FLOAT8.byteValue());
            } else {
               var1.writeByte(TDSType.FLOATN.byteValue());
               var1.writeByte((byte)8);
            }
            break;
         case 91:
            if (!var9 && null != this.sourceBulkRecord) {
               var1.writeByte(TDSType.BIGVARCHAR.byteValue());
               var1.writeShort((short)var4);
               var6.writeCollation(var1);
            } else {
               var1.writeByte(TDSType.DATEN.byteValue());
            }
            break;
         case 92:
            if (!var9 && null != this.sourceBulkRecord) {
               var1.writeByte(TDSType.BIGVARCHAR.byteValue());
               var1.writeShort((short)var4);
               var6.writeCollation(var1);
            } else {
               var1.writeByte(TDSType.TIMEN.byteValue());
               var1.writeByte((byte)var3);
            }
            break;
         case 2013:
         case 2014:
            var1.writeByte(TDSType.DATETIMEOFFSETN.byteValue());
            var1.writeByte((byte)var3);
            break;
         default:
            MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_BulkTypeNotSupported"));
            String var11 = JDBCType.of(var2).toString().toLowerCase(Locale.ENGLISH);
            throw new SQLServerException(var10.format(new Object[]{var11}), null, 0, null);
      }
   }

   private void writeCekTable(TDSWriter var1) throws SQLServerException {
      if (this.connection.getServerSupportsColumnEncryption()) {
         if (null != this.destCekTable && 0 < this.destCekTable.getSize()) {
            var1.writeShort((short)this.destCekTable.getSize());

            for (int var2 = 0; var2 < this.destCekTable.getSize(); var2++) {
               var1.writeInt(((EncryptionKeyInfo)this.destCekTable.getCekTableEntry(var2).getColumnEncryptionKeyValues().get(0)).databaseId);
               var1.writeInt(((EncryptionKeyInfo)this.destCekTable.getCekTableEntry(var2).getColumnEncryptionKeyValues().get(0)).cekId);
               var1.writeInt(((EncryptionKeyInfo)this.destCekTable.getCekTableEntry(var2).getColumnEncryptionKeyValues().get(0)).cekVersion);
               var1.writeBytes(((EncryptionKeyInfo)this.destCekTable.getCekTableEntry(var2).getColumnEncryptionKeyValues().get(0)).cekMdVersion);
               var1.writeByte((byte)0);
            }
         } else {
            var1.writeShort((short)0);
         }
      }
   }

   private final void writeColumnMetaData(TDSWriter var1) throws SQLServerException {
      var1.writeByte((byte)-127);
      byte[] var2 = new byte[]{(byte)(this.columnMappings.size() & 0xFF), (byte)(this.columnMappings.size() >> 8 & 0xFF)};
      var1.writeBytes(var2);
      this.writeCekTable(var1);

      for (int var3 = 0; var3 < this.columnMappings.size(); var3++) {
         this.writeColumnMetaDataColumnData(var1, var3);
      }
   }

   private void checkForTimeoutException(SQLException var1, SQLServerBulkCopy.BulkTimeoutTimer var2) throws SQLServerException {
      if (null != var1.getSQLState() && var1.getSQLState().equals(SQLState.STATEMENT_CANCELED.getSQLStateCode()) && var2.expired()) {
         if (this.copyOptions.isUseInternalTransaction()) {
            this.connection.rollback();
         }

         throw new SQLServerException(SQLServerException.getErrString("R_queryTimedOut"), SQLState.STATEMENT_CANCELED, DriverError.NOT_SET, null);
      }
   }

   private void validateDataTypeConversions(int var1, int var2) throws SQLServerException {
      CryptoMetadata var3 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1)).cryptoMeta;
      CryptoMetadata var4 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).cryptoMeta;
      JDBCType var5 = null != var3
         ? var3.baseTypeInfo.getSSType().getJDBCType()
         : JDBCType.of(((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1)).jdbcType);
      SSType var6 = null != var4 ? var4.baseTypeInfo.getSSType() : ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).ssType;
      if (!var5.convertsTo(var6)) {
         DataTypes.throwConversionError(var5.toString(), var6.toString());
      }
   }

   private String getDestTypeFromSrcType(int var1, int var2, TDSWriter var3) throws SQLServerException {
      SSType var5 = null != ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).cryptoMeta
         ? ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).cryptoMeta.baseTypeInfo.getSSType()
         : ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).ssType;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;
      var6 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1)).jdbcType;
      var7 = var9 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1)).precision;
      int var10 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).precision;
      var8 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1)).scale;
      CryptoMetadata var11 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).cryptoMeta;
      if (null != var11) {
         var3.setCryptoMetaData(((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).cryptoMeta);
         return 8000 < var10 ? "varbinary(max)" : "varbinary(" + ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).precision + ")";
      }

      if (null != this.sourceResultSet
         && null != ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).encryptionType
         && this.copyOptions.isAllowEncryptedValueModifications()) {
         return "varbinary(" + var7 + ")";
      }

      var7 = this.validateSourcePrecision(var9, var6, var10);
      if (this.sourceResultSet instanceof SQLServerResultSet && this.connection.isColumnEncryptionSettingEnabled()) {
         var6 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).jdbcType;
         var7 = var10;
         var8 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2)).scale;
      }

      boolean var4;
      if (-15 != var6 && -9 != var6 && -16 != var6) {
         var4 = 8000 < var9 || 8000 < var10;
      } else {
         var4 = 4000 < var9 || 4000 < var10;
      }

      if (!Util.isCharType(var6) || !Util.isBinaryType(var5)) {
         switch (var6) {
            case -155:
               if (null != this.sourceBulkRecord) {
                  return "varchar(" + (0 == var7 ? var10 : var7) + ")";
               }

               return "datetimeoffset(" + var8 + ")";
            case -151:
            case -150:
            case 93:
               switch (var5) {
                  case SMALLDATETIME:
                     if (null != this.sourceBulkRecord) {
                        return "varchar(" + (0 == var7 ? var10 : var7) + ")";
                     }

                     return "smalldatetime";
                  case DATETIME:
                     if (null != this.sourceBulkRecord) {
                        return "varchar(" + (0 == var7 ? var10 : var7) + ")";
                     }

                     return "datetime";
                  default:
                     if (null != this.sourceBulkRecord) {
                        return "varchar(" + (0 == var7 ? var10 : var7) + ")";
                     }

                     return "datetime2(" + var8 + ")";
               }
            case -148:
            case -146:
            case 3:
               return "decimal(" + var7 + ", " + var8 + ")";
            case -145:
            case 1:
               return "char(" + var7 + ")";
            case -16:
            case -9:
               if (var4) {
                  return "NVARCHAR(MAX)";
               }

               return "NVARCHAR(" + var7 + ")";
            case -15:
               return "NCHAR(" + var7 + ")";
            case -7:
               return "bit";
            case -6:
               return "tinyint";
            case -5:
               return "bigint";
            case -4:
            case -3:
               if (var4) {
                  return "varbinary(max)";
               }

               return "varbinary(" + var7 + ")";
            case -2:
               return "binary(" + var7 + ")";
            case -1:
            case 12:
               if (var4) {
                  return "varchar(max)";
               }

               return "varchar(" + var7 + ")";
            case 2:
               return "numeric(" + var7 + ", " + var8 + ")";
            case 4:
               return "int";
            case 5:
               return "smallint";
            case 7:
               return "real";
            case 8:
               return "float";
            case 91:
               if (null != this.sourceBulkRecord) {
                  return "varchar(" + (0 == var7 ? var10 : var7) + ")";
               }

               return "date";
            case 92:
               if (null != this.sourceBulkRecord) {
                  return "varchar(" + (0 == var7 ? var10 : var7) + ")";
               }

               return "time(" + var8 + ")";
            case 2013:
            case 2014:
               return "datetimeoffset(" + var8 + ")";
            default:
               MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_BulkTypeNotSupported"));
               Object[] var13 = new Object[]{JDBCType.of(var6).toString().toLowerCase(Locale.ENGLISH)};
               SQLServerException.makeFromDriverError(null, null, var12.format(var13), null, true);
               return null;
         }
      } else {
         return var4 ? "varbinary(max)" : var5.toString() + "(" + (8000 < var10 ? "max" : var10) + ")";
      }
   }

   private final String createInsertBulkCommand(TDSWriter var1) throws SQLServerException {
      StringBuilder var2 = new StringBuilder();
      Vector var3 = new Vector();
      String var4 = " , ";
      var2.append("INSERT BULK " + this.destinationTableName + " (");

      for (int var5 = 0; var5 < this.columnMappings.size(); var5++) {
         if (var5 == this.columnMappings.size() - 1) {
            var4 = " ) ";
         }

         SQLServerBulkCopy.ColumnMapping var6 = (SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var5);
         String var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata
               .get(((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var5)).destinationColumnOrdinal))
            .collationName;
         String var8 = "";
         String var9 = this.getDestTypeFromSrcType(var6.sourceColumnOrdinal, var6.destinationColumnOrdinal, var1).toUpperCase(Locale.ENGLISH);
         if (null != var7
            && var7.trim().length() > 0
            && null != var9
            && (var9.toLowerCase().trim().startsWith("char") || var9.toLowerCase().trim().startsWith("varchar"))) {
            var8 = " COLLATE " + var7;
         }

         var2.append("[" + var6.destinationColumnName + "] " + var9 + var8 + var4);
      }

      if (this.copyOptions.isCheckConstraints()) {
         var3.add("CHECK_CONSTRAINTS");
      }

      if (this.copyOptions.isFireTriggers()) {
         var3.add("FIRE_TRIGGERS");
      }

      if (this.copyOptions.isKeepNulls()) {
         var3.add("KEEP_NULLS");
      }

      if (this.copyOptions.getBatchSize() > 0) {
         var3.add("ROWS_PER_BATCH = " + this.copyOptions.getBatchSize());
      }

      if (this.copyOptions.isTableLock()) {
         var3.add("TABLOCK");
      }

      if (this.copyOptions.isAllowEncryptedValueModifications()) {
         var3.add("ALLOW_ENCRYPTED_VALUE_MODIFICATIONS");
      }

      Iterator var10 = var3.iterator();
      if (var10.hasNext()) {
         var2.append(" with (");

         while (var10.hasNext()) {
            var2.append(((String)var10.next()).toString());
            if (var10.hasNext()) {
               var2.append(", ");
            }
         }

         var2.append(")");
      }

      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.finer(this.toString() + " TDSCommand: " + var2);
      }

      return var2.toString();
   }

   private final boolean doInsertBulk(TDSCommand var1) throws SQLServerException {
      if (this.copyOptions.isUseInternalTransaction()) {
         this.connection.setAutoCommit(false);
      }

      TDSWriter var2 = var1.startRequest((byte)1);
      String var3 = this.createInsertBulkCommand(var2);
      var2.writeString(var3);
      TDSParser.parse(var1.startResponse(), var1.getLogContext());
      var2 = var1.startRequest((byte)7);
      boolean var4 = false;

      try {
         this.writeColumnMetaData(var2);
         var4 = this.writeBatchData(var2);
      } catch (SQLServerException var9) {
         this.writePacketDataDone(var2);
         var1.startRequest((byte)6);
         TDSParser.parse(var1.startResponse(), var1.getLogContext());
         var1.interrupt(var9.getMessage());
         var1.onRequestComplete();
         throw var9;
      } finally {
         var2.setCryptoMetaData(null);
      }

      this.writePacketDataDone(var2);
      TDSParser.parse(var1.startResponse(), var1.getLogContext());
      if (this.copyOptions.isUseInternalTransaction()) {
         this.connection.commit();
      }

      return var4;
   }

   private final void writePacketDataDone(TDSWriter var1) throws SQLServerException {
      var1.writeByte((byte)-3);
      var1.writeLong(0L);
      var1.writeInt(0);
   }

   private void throwInvalidArgument(String var1) throws SQLServerException {
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
      Object[] var3 = new Object[]{var1};
      SQLServerException.makeFromDriverError(null, null, var2.format(var3), null, false);
   }

   private void throwInvalidJavaToJDBC(String var1, int var2) throws SQLServerException {
      MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
      throw new SQLServerException(var3.format(new Object[]{var1, var2}), null, 0, null);
   }

   private void writeToServer() throws SQLServerException {
      if (this.connection.isClosed()) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_connectionIsClosed"), "08003", false);
      }

      long var1 = System.currentTimeMillis();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.finer(this.toString() + " Start writeToServer: " + var1);
      }

      this.getDestinationMetadata();
      this.getSourceMetadata();
      this.validateColumnMappings();
      this.sendBulkLoadBCP();
      long var3 = System.currentTimeMillis();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.finer(this.toString() + " End writeToServer: " + var3);
         int var5 = (int)((var3 - var1) / 1000L);
         loggerExternal.finer(this.toString() + "Time elapsed: " + var5 + " seconds");
      }
   }

   private void validateStringBinaryLengths(Object var1, int var2, int var3) throws SQLServerException {
      int var4 = 0;
      int var5 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var3)).precision;
      int var6 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).jdbcType;
      SSType var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var3)).ssType;
      if (Util.isCharType(var6) && Util.isCharType(var7) || Util.isBinaryType(var6) && Util.isBinaryType(var7)) {
         if (var1 instanceof String) {
            var4 = ((String)var1).length();
         } else {
            if (!(var1 instanceof byte[])) {
               return;
            }

            var4 = ((byte[])var1).length;
         }

         if (var4 > var5) {
            String var8 = JDBCType.of(var6) + "(" + var4 + ")";
            String var9 = var7.toString() + "(" + var5 + ")";
            MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
            Object[] var11 = new Object[]{var8, var9};
            throw new SQLServerException(this, var10.format(var11), null, 0, false);
         }
      }
   }

   private void getDestinationMetadata() throws SQLServerException {
      if (null == this.destinationTableName) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDestinationTable"), null, false);
      }

      SQLServerResultSet var1 = null;
      SQLServerResultSet var2 = null;

      try {
         var1 = ((SQLServerStatement)this.connection.createStatement())
            .executeQueryInternal("SET FMTONLY ON SELECT * FROM " + this.destinationTableName + " SET FMTONLY OFF ");
         this.destColumnCount = var1.getMetaData().getColumnCount();
         this.destColumnMetadata = new HashMap();
         this.destCekTable = var1.getCekTable();
         if (!this.connection.getServerSupportsColumnEncryption()) {
            var2 = ((SQLServerStatement)this.connection.createStatement())
               .executeQueryInternal(
                  "select collation_name from sys.columns where object_id=OBJECT_ID('" + this.destinationTableName + "') " + "order by column_id ASC"
               );
         } else {
            var2 = ((SQLServerStatement)this.connection.createStatement())
               .executeQueryInternal(
                  "select collation_name, encryption_type from sys.columns where object_id=OBJECT_ID('"
                     + this.destinationTableName
                     + "') "
                     + "order by column_id ASC"
               );
         }

         for (int var3 = 1; var3 <= this.destColumnCount; var3++) {
            if (var2.next()) {
               if (!this.connection.getServerSupportsColumnEncryption()) {
                  this.destColumnMetadata.put(var3, new SQLServerBulkCopy.BulkColumnMetaData(var1.getColumn(var3), var2.getString("collation_name"), null));
               } else {
                  this.destColumnMetadata
                     .put(
                        var3,
                        new SQLServerBulkCopy.BulkColumnMetaData(var1.getColumn(var3), var2.getString("collation_name"), var2.getString("encryption_type"))
                     );
               }
            } else {
               this.destColumnMetadata.put(var3, new SQLServerBulkCopy.BulkColumnMetaData(var1.getColumn(var3)));
            }
         }
      } catch (SQLException var7) {
         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), var7);
      } finally {
         if (null != var1) {
            var1.close();
         }

         if (null != var2) {
            var2.close();
         }
      }
   }

   private void getSourceMetadata() throws SQLServerException {
      this.srcColumnMetadata = new HashMap();
      if (null != this.sourceResultSet) {
         try {
            this.srcColumnCount = this.sourceResultSetMetaData.getColumnCount();

            for (int var2 = 1; var2 <= this.srcColumnCount; var2++) {
               this.srcColumnMetadata
                  .put(
                     var2,
                     new SQLServerBulkCopy.BulkColumnMetaData(
                        this.sourceResultSetMetaData.getColumnName(var2),
                        0 != this.sourceResultSetMetaData.isNullable(var2),
                        this.sourceResultSetMetaData.getPrecision(var2),
                        this.sourceResultSetMetaData.getScale(var2),
                        this.sourceResultSetMetaData.getColumnType(var2),
                        null
                     )
                  );
            }
         } catch (SQLException var4) {
            throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), var4);
         }
      } else {
         if (null == this.sourceBulkRecord) {
            throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), null);
         }

         Set var5 = this.sourceBulkRecord.getColumnOrdinals();
         this.srcColumnCount = var5.size();
         if (0 == this.srcColumnCount) {
            throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), null);
         }

         for (int var1 : var5) {
            this.srcColumnMetadata
               .put(
                  var1,
                  new SQLServerBulkCopy.BulkColumnMetaData(
                     this.sourceBulkRecord.getColumnName(var1),
                     true,
                     this.sourceBulkRecord.getPrecision(var1),
                     this.sourceBulkRecord.getScale(var1),
                     this.sourceBulkRecord.getColumnType(var1),
                     this.sourceBulkRecord instanceof SQLServerBulkCSVFileRecord
                        ? ((SQLServerBulkCSVFileRecord)this.sourceBulkRecord).getColumnDateTimeFormatter(var1)
                        : null
                  )
               );
         }
      }
   }

   private int validateSourcePrecision(int var1, int var2, int var3) {
      if (1 > var1 && Util.isCharType(var2)) {
         var1 = var3;
      }

      return var1;
   }

   private void validateColumnMappings() throws SQLServerException {
      try {
         if (this.columnMappings.isEmpty()) {
            if (this.destColumnCount != this.srcColumnCount) {
               throw new SQLServerException(SQLServerException.getErrString("R_schemaMismatch"), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
            }

            for (int var11 = 1; var11 <= this.srcColumnCount; var11++) {
               if (!((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var11)).isIdentity || this.copyOptions.isKeepIdentity()) {
                  SQLServerBulkCopy.ColumnMapping var9 = new SQLServerBulkCopy.ColumnMapping(var11, var11);
                  var9.destinationColumnName = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var11)).columnName;
                  this.columnMappings.add(var9);
               }
            }

            if (null != this.sourceBulkRecord) {
               Set var12 = this.sourceBulkRecord.getColumnOrdinals();
               Iterator var14 = var12.iterator();

               for (int var18 = 1; var14.hasNext(); var18++) {
                  int var26 = (Integer)var14.next();
                  if (var18 != var26) {
                     MessageFormat var31 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
                     Object[] var32 = new Object[]{var26};
                     throw new SQLServerException(var31.format(var32), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
                  }
               }
            }
         } else {
            int var1 = this.columnMappings.size();

            for (int var3 = 0; var3 < var1; var3++) {
               SQLServerBulkCopy.ColumnMapping var2 = (SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var3);
               if (-1 == var2.destinationColumnOrdinal) {
                  boolean var4 = false;

                  for (int var5 = 1; var5 <= this.destColumnCount; var5++) {
                     if (((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var5)).columnName.equals(var2.destinationColumnName)) {
                        var4 = true;
                        var2.destinationColumnOrdinal = var5;
                        break;
                     }
                  }

                  if (!var4) {
                     MessageFormat var19 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
                     Object[] var6 = new Object[]{var2.destinationColumnName};
                     throw new SQLServerException(var19.format(var6), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
                  }
               } else {
                  if (0 > var2.destinationColumnOrdinal || this.destColumnCount < var2.destinationColumnOrdinal) {
                     MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
                     Object[] var20 = new Object[]{var2.destinationColumnOrdinal};
                     throw new SQLServerException(var15.format(var20), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
                  }

                  var2.destinationColumnName = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var2.destinationColumnOrdinal)).columnName;
               }
            }

            for (int var13 = 0; var13 < var1; var13++) {
               SQLServerBulkCopy.ColumnMapping var10 = (SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var13);
               if (-1 != var10.sourceColumnOrdinal) {
                  boolean var17 = true;
                  if (null != this.sourceResultSet) {
                     int var24 = this.sourceResultSetMetaData.getColumnCount();
                     if (0 < var10.sourceColumnOrdinal && var24 >= var10.sourceColumnOrdinal) {
                        var17 = false;
                     }
                  } else if (this.srcColumnMetadata.containsKey(var10.sourceColumnOrdinal)) {
                     var17 = false;
                  }

                  if (var17) {
                     MessageFormat var25 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
                     Object[] var30 = new Object[]{var10.sourceColumnOrdinal};
                     throw new SQLServerException(var25.format(var30), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
                  }
               } else {
                  boolean var16 = false;
                  if (null != this.sourceResultSet) {
                     int var21 = this.sourceResultSetMetaData.getColumnCount();

                     for (int var27 = 1; var27 <= var21; var27++) {
                        if (this.sourceResultSetMetaData.getColumnName(var27).equals(var10.sourceColumnName)) {
                           var16 = true;
                           var10.sourceColumnOrdinal = var27;
                           break;
                        }
                     }
                  } else {
                     for (int var7 : this.sourceBulkRecord.getColumnOrdinals()) {
                        if (this.sourceBulkRecord.getColumnName(var7).equals(var10.sourceColumnName)) {
                           var16 = true;
                           var10.sourceColumnOrdinal = var7;
                           break;
                        }
                     }
                  }

                  if (!var16) {
                     MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_invalidColumn"));
                     Object[] var29 = new Object[]{var10.sourceColumnName};
                     throw new SQLServerException(var23.format(var29), SQLState.COL_NOT_FOUND, DriverError.NOT_SET, null);
                  }
               }

               if (((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var10.destinationColumnOrdinal)).isIdentity
                  && !this.copyOptions.isKeepIdentity()) {
                  this.columnMappings.remove(var13);
                  var1--;
                  var13--;
               }
            }
         }
      } catch (SQLException var8) {
         if (var8 instanceof SQLServerException && null != var8.getSQLState() && var8.getSQLState().equals(SQLState.COL_NOT_FOUND.getSQLStateCode())) {
            throw (SQLServerException)var8;
         }

         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), var8);
      }

      if (this.columnMappings.isEmpty()) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_BulkColumnMappingsIsEmpty"), null, 0, false);
      }
   }

   private void writeNullToTdsWriter(TDSWriter var1, int var2, boolean var3) throws SQLServerException {
      switch (var2) {
         case -155:
         case -7:
         case -6:
         case -5:
         case 2:
         case 3:
         case 4:
         case 5:
         case 7:
         case 8:
         case 91:
         case 92:
         case 93:
         case 2013:
         case 2014:
            var1.writeByte((byte)0);
            return;
         case -16:
         case -15:
         case -9:
         case -4:
         case -3:
         case -2:
         case -1:
         case 1:
         case 12:
            if (var3) {
               var1.writeLong(-1L);
            } else {
               var1.writeByte((byte)-1);
               var1.writeByte((byte)-1);
            }

            return;
         default:
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_BulkTypeNotSupported"));
            Object[] var5 = new Object[]{JDBCType.of(var2).toString().toLowerCase(Locale.ENGLISH)};
            SQLServerException.makeFromDriverError(null, null, var4.format(var5), null, true);
      }
   }

   private void writeColumnToTdsWriter(TDSWriter var1, int var2, int var3, int var4, boolean var5, int var6, int var7, boolean var8, Object var9) throws SQLServerException {
      SSType var10 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).ssType;
      var2 = this.validateSourcePrecision(var2, var4, ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).precision);
      CryptoMetadata var11 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var6)).cryptoMeta;
      if ((
            null == ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).encryptionType
               || !this.copyOptions.isAllowEncryptedValueModifications()
         )
         && null == ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).cryptoMeta) {
         if (null != var11) {
            var4 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).jdbcType;
            var3 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).scale;
         } else if (null != this.sourceBulkRecord) {
            switch (var4) {
               case -155:
               case 91:
               case 92:
               case 93:
                  var4 = 12;
            }
         }
      } else {
         var4 = -3;
      }

      switch (var4) {
         case -155:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (2 >= var3) {
                  var1.writeByte((byte)8);
               } else if (4 >= var3) {
                  var1.writeByte((byte)9);
               } else {
                  var1.writeByte((byte)10);
               }

               var1.writeDateTimeOffset((DateTimeOffset)var9, var3, var10);
            }
            break;
         case -151:
         case -150:
         case 93:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               switch (var10) {
                  case SMALLDATETIME:
                     if (var5) {
                        var1.writeByte((byte)4);
                     }

                     var1.writeSmalldatetime(var9.toString());
                     return;
                  case DATETIME:
                     if (var5) {
                        var1.writeByte((byte)8);
                     }

                     var1.writeDatetime(var9.toString());
                     return;
                  default:
                     if (var5) {
                        if (2 >= var3) {
                           var1.writeByte((byte)6);
                        } else if (4 >= var3) {
                           var1.writeByte((byte)7);
                        } else {
                           var1.writeByte((byte)8);
                        }
                     }

                     String var31 = var9.toString();
                     var1.writeTime(Timestamp.valueOf(var31), var3);
                     var1.writeDate(var31.substring(0, var31.lastIndexOf(32)));
               }
            }
            break;
         case -148:
         case -146:
         case 2:
         case 3:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               var1.writeBigDecimal((BigDecimal)var9, var4, var2);
            }
            break;
         case -145:
         case -1:
         case 1:
         case 12:
            if (var8) {
               if (null == var9) {
                  this.writeNullToTdsWriter(var1, var4, var8);
               } else {
                  var1.writeLong(-2L);

                  try {
                     Object var28 = null;
                     if (var9 instanceof Reader) {
                        var28 = (Reader)var9;
                     } else {
                        var28 = new StringReader(var9.toString());
                     }

                     if (SSType.BINARY != var10 && SSType.VARBINARY != var10 && SSType.VARBINARYMAX != var10 && SSType.IMAGE != var10) {
                        SQLCollation var33 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).collation;
                        if (null != var33) {
                           var1.writeNonUnicodeReader((Reader)var28, -1L, false, var33.getCharset());
                        } else {
                           var1.writeNonUnicodeReader((Reader)var28, -1L, false, null);
                        }
                     } else {
                        var1.writeNonUnicodeReader((Reader)var28, -1L, true, null);
                     }

                     var28.close();
                  } catch (IOException var20) {
                     throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var20);
                  }
               }
            } else if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               String var30 = var9.toString();
               if (SSType.BINARY != var10 && SSType.VARBINARY != var10) {
                  try {
                     var1.writeShort((short)var30.length());
                     SQLCollation var36 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).collation;
                     if (null != var36) {
                        var1.writeBytes(var30.getBytes(((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var7)).collation.getCharset()));
                     } else {
                        var1.writeBytes(var30.getBytes());
                     }
                  } catch (UnsupportedEncodingException var18) {
                     throw new SQLServerException(SQLServerException.getErrString("R_encodingErrorWritingTDS"), var18);
                  }
               } else {
                  byte[] var34 = null;

                  try {
                     var34 = ParameterUtils.HexToBin(var30);
                  } catch (SQLServerException var19) {
                     throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var19);
                  }

                  var1.writeShort((short)var34.length);
                  var1.writeBytes(var34);
               }
            }
            break;
         case -16:
         case -15:
         case -9:
            if (var8) {
               if (null == var9) {
                  this.writeNullToTdsWriter(var1, var4, var8);
               } else {
                  var1.writeLong(-2L);

                  try {
                     Object var25 = null;
                     if (var9 instanceof Reader) {
                        var25 = (Reader)var9;
                     } else {
                        var25 = new StringReader(var9.toString());
                     }

                     var1.writeReader((Reader)var25, -1L, true);
                     var25.close();
                  } catch (IOException var17) {
                     throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var17);
                  }
               }
            } else if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               int var27 = var9.toString().length();
               byte[] var32 = new byte[]{(byte)(2 * var27 & 0xFF), (byte)(2 * var27 >> 8 & 0xFF)};
               var1.writeBytes(var32);
               var1.writeString(var9.toString());
            }
            break;
         case -7:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)1);
               }

               var1.writeByte((byte)((Boolean)var9 ? 1 : 0));
            }
            break;
         case -6:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)1);
               }

               var1.writeByte((byte)(((Number)var9).shortValue() & 0xFF));
            }
            break;
         case -5:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)8);
               }

               var1.writeLong((Long)var9);
            }
            break;
         case -4:
         case -3:
         case -2:
            if (var8) {
               if (null == var9) {
                  this.writeNullToTdsWriter(var1, var4, var8);
               } else {
                  var1.writeLong(-2L);

                  try {
                     Object var22 = null;
                     if (var9 instanceof InputStream) {
                        var22 = (InputStream)var9;
                     } else if (var9 instanceof byte[]) {
                        var22 = new ByteArrayInputStream((byte[])var9);
                     } else {
                        var22 = new ByteArrayInputStream(ParameterUtils.HexToBin(var9.toString()));
                     }

                     var1.writeStream((InputStream)var22, -1L, true);
                     var22.close();
                  } catch (IOException var16) {
                     throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var16);
                  }
               }
            } else if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               byte[] var24;
               if (var9 instanceof byte[]) {
                  var24 = (byte[])var9;
               } else {
                  try {
                     var24 = ParameterUtils.HexToBin(var9.toString());
                  } catch (SQLServerException var15) {
                     throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var15);
                  }
               }

               var1.writeShort((short)var24.length);
               var1.writeBytes(var24);
            }
            break;
         case 4:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)4);
               }

               var1.writeInt((Integer)var9);
            }
            break;
         case 5:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)2);
               }

               var1.writeShort(((Number)var9).shortValue());
            }
            break;
         case 7:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)4);
               }

               var1.writeReal((Float)var9);
            }
            break;
         case 8:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (var5) {
                  var1.writeByte((byte)8);
               }

               var1.writeDouble((Double)var9);
            }
            break;
         case 91:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               var1.writeByte((byte)3);
               var1.writeDate(var9.toString());
            }
            break;
         case 92:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (2 >= var3) {
                  var1.writeByte((byte)3);
               } else if (4 >= var3) {
                  var1.writeByte((byte)4);
               } else {
                  var1.writeByte((byte)5);
               }

               var1.writeTime((Timestamp)var9, var3);
            }
            break;
         case 2013:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (2 >= var3) {
                  var1.writeByte((byte)8);
               } else if (4 >= var3) {
                  var1.writeByte((byte)9);
               } else {
                  var1.writeByte((byte)10);
               }

               var1.writeOffsetTimeWithTimezone((OffsetTime)var9, var3);
            }
            break;
         case 2014:
            if (null == var9) {
               this.writeNullToTdsWriter(var1, var4, var8);
            } else {
               if (2 >= var3) {
                  var1.writeByte((byte)8);
               } else if (4 >= var3) {
                  var1.writeByte((byte)9);
               } else {
                  var1.writeByte((byte)10);
               }

               var1.writeOffsetDateTimeWithTimezone((OffsetDateTime)var9, var3);
            }
            break;
         default:
            MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_BulkTypeNotSupported"));
            Object[] var13 = new Object[]{JDBCType.of(var4).toString().toLowerCase(Locale.ENGLISH)};
            SQLServerException.makeFromDriverError(null, null, var12.format(var13), null, true);
      }
   }

   private Object readColumnFromResultSet(int var1, int var2, boolean var3, boolean var4) throws SQLServerException {
      CryptoMetadata var5 = null;
      if (this.sourceResultSet instanceof SQLServerResultSet
         && null != (var5 = ((SQLServerResultSet)this.sourceResultSet).getterGetColumn(var1).getCryptoMetadata())) {
         var2 = var5.baseTypeInfo.getSSType().getJDBCType().asJavaSqlType();
         SQLServerBulkCopy.BulkColumnMetaData var6 = (SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var1);
         this.srcColumnMetadata.put(var1, new SQLServerBulkCopy.BulkColumnMetaData(var6, var5));
      }

      try {
         switch (var2) {
            case -155:
               return ((SQLServerResultSet)this.sourceResultSet).getDateTimeOffset(var1);
            case -151:
            case -150:
            case 93:
               return this.sourceResultSet.getTimestamp(var1);
            case -148:
            case -146:
            case 2:
            case 3:
               return this.sourceResultSet.getBigDecimal(var1);
            case -145:
            case -1:
            case 1:
            case 12:
               if (var3 && !var4) {
                  return this.sourceResultSet.getCharacterStream(var1);
               }

               return this.sourceResultSet.getString(var1);
            case -16:
            case -15:
            case -9:
               if (var3 && !var4) {
                  return this.sourceResultSet.getNCharacterStream(var1);
               }

               return this.sourceResultSet.getObject(var1);
            case -7:
            case -6:
            case -5:
            case 4:
            case 5:
            case 7:
            case 8:
               return this.sourceResultSet.getObject(var1);
            case -4:
            case -3:
            case -2:
               if (var3 && !var4) {
                  return this.sourceResultSet.getBinaryStream(var1);
               }

               return this.sourceResultSet.getBytes(var1);
            case 91:
               return this.sourceResultSet.getDate(var1);
            case 92:
               return this.sourceResultSet.getTimestamp(var1);
            default:
               MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_BulkTypeNotSupported"));
               Object[] var7 = new Object[]{JDBCType.of(var2).toString().toLowerCase(Locale.ENGLISH)};
               SQLServerException.makeFromDriverError(null, null, var10.format(var7), null, true);
               return null;
         }
      } catch (SQLException var8) {
         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var8);
      }
   }

   private final void writeColumn(TDSWriter var1, int var2, int var3, Object var4) throws SQLServerException {
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      SSType var9 = null;
      boolean var10 = false;
      var5 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).precision;
      var6 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).scale;
      var8 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).jdbcType;
      boolean var11 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).isNullable;
      var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var3)).precision;
      if (-15 != var8 && -9 != var8 && -16 != var8) {
         var10 = 8000 < var5 || 8000 < var7;
      } else {
         var10 = 4000 < var5 || 4000 < var7;
      }

      CryptoMetadata var12 = ((SQLServerBulkCopy.BulkColumnMetaData)this.destColumnMetadata.get(var3)).cryptoMeta;
      if (null != var12) {
         var9 = var12.baseTypeInfo.getSSType();
      }

      if (null != this.sourceResultSet) {
         var4 = this.readColumnFromResultSet(var2, var8, var10, null != var12);
         this.validateStringBinaryLengths(var4, var2, var3);
         if (!this.copyOptions.isAllowEncryptedValueModifications() && (null == var12 || null == var4)) {
            this.validateDataTypeConversions(var2, var3);
         }
      } else if (null != this.sourceBulkRecord && null != var12) {
         if (91 != var8 && 92 != var8 && 93 != var8 && -155 != var8 && 2013 != var8 && 2014 != var8) {
            if (2 == var8 || 3 == var8) {
               int var13 = var12.baseTypeInfo.getPrecision();
               int var14 = var12.baseTypeInfo.getScale();
               if (var6 != var14 || var5 != var13) {
                  MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
                  String var16 = JDBCType.of(var8) + "(" + var5 + "," + var6 + ")";
                  String var17 = var9 + "(" + var13 + "," + var14 + ")";
                  Object[] var18 = new Object[]{var16, var17};
                  throw new SQLServerException(this, var15.format(var18), null, 0, false);
               }
            }
         } else {
            var4 = this.getTemporalObjectFromCSV(var4, var8, var2);
         }
      }

      CryptoMetadata var24 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).cryptoMeta;
      if (null != var12 && null != var4) {
         JDBCType var25 = null != var24
            ? ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var2)).cryptoMeta.baseTypeInfo.getSSType().getJDBCType()
            : JDBCType.of(var8);
         if (JDBCType.TIMESTAMP == var25) {
            if (SSType.DATETIME == var9) {
               var25 = JDBCType.DATETIME;
            } else if (SSType.SMALLDATETIME == var9) {
               var25 = JDBCType.SMALLDATETIME;
            }
         }

         if ((SSType.MONEY != var9 || JDBCType.DECIMAL != var25)
            && (SSType.SMALLMONEY != var9 || JDBCType.DECIMAL != var25)
            && (SSType.GUID != var9 || JDBCType.CHAR != var25)
            && (!Util.isCharType(var9) || !Util.isCharType(var8))
            && !(this.sourceResultSet instanceof SQLServerResultSet)
            && !var25.normalizationCheck(var9)) {
            MessageFormat var27 = new MessageFormat(SQLServerException.getErrString("R_unsupportedConversionAE"));
            Object[] var28 = new Object[]{var25, var9};
            throw new SQLServerException(this, var27.format(var28), null, 0, false);
         }

         if (var25 != JDBCType.DATE
            && var25 != JDBCType.TIMESTAMP
            && var25 != JDBCType.TIME
            && var25 != JDBCType.DATETIMEOFFSET
            && var25 != JDBCType.DATETIME
            && var25 != JDBCType.SMALLDATETIME) {
            TypeInfo var26 = var12.getBaseTypeInfo();
            var4 = SQLServerSecurityUtility.encryptWithKey(
               this.normalizedValue(var26.getSSType().getJDBCType(), var4, var25, var26.getPrecision(), var26.getScale()), var12, this.connection
            );
         } else {
            var4 = this.getEncryptedTemporalBytes(var1, var25, var4, var2, var12.baseTypeInfo.getScale());
         }
      }

      this.writeColumnToTdsWriter(var1, var5, var6, var8, var11, var2, var3, var10, var4);
   }

   private Object getTemporalObjectFromCSVWithFormatter(String var1, int var2, int var3, DateTimeFormatter var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();

      try {
         TemporalAccessor var5 = var4.parse(var1);
         int var13 = 0;
         int var12 = 0;
         int var11 = 0;
         int var10 = 0;
         int var9 = 0;
         int var8 = 0;
         int var19 = 0;
         int var18 = 0;
         if (var5.isSupported(ChronoField.NANO_OF_SECOND)) {
            var12 = var5.get(ChronoField.NANO_OF_SECOND);
         }

         if (var5.isSupported(ChronoField.OFFSET_SECONDS)) {
            var13 = var5.get(ChronoField.OFFSET_SECONDS);
         }

         if (var5.isSupported(ChronoField.HOUR_OF_DAY)) {
            var18 = var5.get(ChronoField.HOUR_OF_DAY);
         }

         if (var5.isSupported(ChronoField.MINUTE_OF_HOUR)) {
            var19 = var5.get(ChronoField.MINUTE_OF_HOUR);
         }

         if (var5.isSupported(ChronoField.SECOND_OF_MINUTE)) {
            var8 = var5.get(ChronoField.SECOND_OF_MINUTE);
         }

         if (var5.isSupported(ChronoField.DAY_OF_MONTH)) {
            var11 = var5.get(ChronoField.DAY_OF_MONTH);
         }

         if (var5.isSupported(ChronoField.MONTH_OF_YEAR)) {
            var10 = var5.get(ChronoField.MONTH_OF_YEAR);
         }

         if (var5.isSupported(ChronoField.YEAR)) {
            var9 = var5.get(ChronoField.YEAR);
         }

         GregorianCalendar var14 = null;
         var14 = new GregorianCalendar(new SimpleTimeZone(var13 * 1000, ""));
         var14.clear();
         var14.set(11, var18);
         var14.set(12, var19);
         var14.set(13, var8);
         var14.set(5, var11);
         var14.set(2, var10 - 1);
         var14.set(1, var9);
         int var15 = Integer.toString(var12).length();

         for (int var16 = 0; var16 < 9 - var15; var16++) {
            var12 *= 10;
         }

         Timestamp var21 = new Timestamp(var14.getTimeInMillis());
         var21.setNanos(var12);
         switch (var2) {
            case -155:
               return DateTimeOffset.valueOf(var21, var13 / 60);
            case 91:
               return new Date(var21.getTime());
            case 92:
               var14.set(this.connection.baseYear(), 0, 1);
               var21 = new Timestamp(var14.getTimeInMillis());
               var21.setNanos(var12);
               return new Timestamp(var21.getTime());
            case 93:
               return var21;
            default:
               return var1;
         }
      } catch (DateTimeException | ArithmeticException var17) {
         MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_ParsingError"));
         Object[] var7 = new Object[]{JDBCType.of(var2)};
         throw new SQLServerException(this, var6.format(var7), null, 0, false);
      }
   }

   private Object getTemporalObjectFromCSV(Object var1, int var2, int var3) throws SQLServerException {
      if (2013 == var2) {
         MessageFormat var28 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
         Object[] var30 = new Object[]{"TIME_WITH_TIMEZONE"};
         throw new SQLServerException(this, var28.format(var30), null, 0, false);
      }

      if (2014 == var2) {
         MessageFormat var27 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
         Object[] var29 = new Object[]{"TIMESTAMP_WITH_TIMEZONE"};
         throw new SQLServerException(this, var27.format(var29), null, 0, false);
      }

      String var4 = null;
      String var5 = null;
      if (null != var1 && var1 instanceof String) {
         var5 = (String)var1;
         var4 = var5.trim();
      }

      if (null == var4) {
         switch (var2) {
            case -155:
               return (DateTimeOffset)null;
            case 91:
               return (Date)null;
            case 92:
            case 93:
               return (Timestamp)null;
         }
      }

      GregorianCalendar var6 = null;
      DateTimeFormatter var7 = ((SQLServerBulkCopy.BulkColumnMetaData)this.srcColumnMetadata.get(var3)).dateTimeFormatter;
      if (null != var7) {
         return this.getTemporalObjectFromCSVWithFormatter(var5, var2, var3, var7);
      }

      try {
         switch (var2) {
            case -155:
               int var32 = var4.indexOf(45, 0);
               int var50 = Integer.parseInt(var4.substring(0, var32));
               int var53 = ++var32;
               var32 = var4.indexOf(45, var53);
               int var11 = Integer.parseInt(var4.substring(var53, var32));
               var53 = ++var32;
               var32 = var4.indexOf(32, var53);
               int var12 = Integer.parseInt(var4.substring(var53, var32));
               var53 = ++var32;
               var32 = var4.indexOf(58, var53);
               int var13 = Integer.parseInt(var4.substring(var53, var32));
               var53 = ++var32;
               var32 = var4.indexOf(58, var53);
               int var14 = Integer.parseInt(var4.substring(var53, var32));
               var53 = ++var32;
               var32 = var4.indexOf(46, var53);
               int var15 = 0;
               int var18 = 0;
               int var19 = 0;
               boolean var20 = false;
               boolean var21 = false;
               int var22 = 0;
               if (-1 != var32) {
                  var15 = Integer.parseInt(var4.substring(var53, var32));
                  var53 = ++var32;
                  var32 = var4.indexOf(32, var53);
                  if (-1 != var32) {
                     var19 = Integer.parseInt(var4.substring(var53, var32));
                     var22 = var32 - var53;
                     var21 = true;
                  } else {
                     var19 = Integer.parseInt(var4.substring(var53));
                     var22 = var4.length() - var53;
                  }
               } else {
                  var32 = var4.indexOf(32, var53);
                  if (-1 != var32) {
                     var21 = true;
                     var15 = Integer.parseInt(var4.substring(var53, var32));
                  } else {
                     var15 = Integer.parseInt(var4.substring(var53));
                     var32++;
                  }
               }

               if (var21) {
                  var53 = ++var32;
                  if ('+' == var4.charAt(var53)) {
                     var53++;
                  } else if ('-' == var4.charAt(var53)) {
                     var20 = true;
                     var53++;
                  }

                  var32 = var4.indexOf(58, var53);
                  int var16 = Integer.parseInt(var4.substring(var53, var32));
                  var53 = ++var32;
                  int var17 = Integer.parseInt(var4.substring(var53));
                  var18 = var16 * 60 + var17;
                  if (var20) {
                     var18 = -var18;
                  }
               }

               var6 = new GregorianCalendar(new SimpleTimeZone(var18 * 60 * 1000, ""), Locale.US);
               var6.clear();
               var6.set(11, var13);
               var6.set(12, var14);
               var6.set(13, var15);
               var6.set(5, var12);
               var6.set(2, var11 - 1);
               var6.set(1, var50);

               for (int var23 = 0; var23 < 9 - var22; var23++) {
                  var19 *= 10;
               }

               Timestamp var62 = new Timestamp(var6.getTimeInMillis());
               var62.setNanos(var19);
               return DateTimeOffset.valueOf(var62, var18);
            case 91:
               return Date.valueOf(var4);
            case 92:
               String var8 = this.connection.baseYear() + "-01-01 " + var4;
               return Timestamp.valueOf(var8);
            case 93:
               return Timestamp.valueOf(var4);
            default:
               return var1;
         }
      } catch (IndexOutOfBoundsException var24) {
         MessageFormat var49 = new MessageFormat(SQLServerException.getErrString("R_ParsingError"));
         Object[] var52 = new Object[]{JDBCType.of(var2)};
         throw new SQLServerException(this, var49.format(var52), null, 0, false);
      } catch (NumberFormatException var25) {
         MessageFormat var48 = new MessageFormat(SQLServerException.getErrString("R_ParsingError"));
         Object[] var51 = new Object[]{JDBCType.of(var2)};
         throw new SQLServerException(this, var48.format(var51), null, 0, false);
      } catch (IllegalArgumentException var26) {
         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_ParsingError"));
         Object[] var10 = new Object[]{JDBCType.of(var2)};
         throw new SQLServerException(this, var9.format(var10), null, 0, false);
      }
   }

   private byte[] getEncryptedTemporalBytes(TDSWriter var1, JDBCType var2, Object var3, int var4, int var5) throws SQLServerException {
      long var6 = 0L;
      GregorianCalendar var8 = null;
      switch (var2) {
         case DATE:
            var8 = new GregorianCalendar(TimeZone.getDefault(), Locale.US);
            var8.setLenient(true);
            var8.clear();
            var8.setTimeInMillis(((Date)var3).getTime());
            return var1.writeEncryptedScaledTemporal(var8, 0, 0, SSType.DATE, (short)0);
         case TIME:
            var8 = new GregorianCalendar(TimeZone.getDefault(), Locale.US);
            var8.setLenient(true);
            var8.clear();
            var6 = ((Timestamp)var3).getTime();
            var8.setTimeInMillis(var6);
            int var25 = 0;
            if (var3 instanceof Timestamp) {
               var25 = ((Timestamp)var3).getNanos();
            } else {
               var25 = 1000000 * (int)(var6 % 1000L);
               if (var25 < 0) {
                  var25 += 1000000000;
               }
            }

            return var1.writeEncryptedScaledTemporal(var8, var25, var5, SSType.TIME, (short)0);
         case TIMESTAMP:
            var8 = new GregorianCalendar(TimeZone.getDefault(), Locale.US);
            var8.setLenient(true);
            var8.clear();
            var6 = ((Timestamp)var3).getTime();
            var8.setTimeInMillis(var6);
            int var24 = ((Timestamp)var3).getNanos();
            return var1.writeEncryptedScaledTemporal(var8, var24, var5, SSType.DATETIME2, (short)0);
         case DATETIME:
         case SMALLDATETIME:
            var8 = new GregorianCalendar(TimeZone.getDefault(), Locale.US);
            var8.setLenient(true);
            var8.clear();
            var6 = ((Timestamp)var3).getTime();
            var8.setTimeInMillis(var6);
            int var23 = ((Timestamp)var3).getNanos();
            return var1.getEncryptedDateTimeAsBytes(var8, var23, var2);
         case DATETIMEOFFSET:
            DateTimeOffset var10 = (DateTimeOffset)var3;
            var6 = var10.getTimestamp().getTime();
            int var9 = var10.getTimestamp().getNanos();
            int var11 = var10.getMinutesOffset();
            var8 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            var8.setLenient(true);
            var8.clear();
            var8.setTimeInMillis(var6);
            return var1.writeEncryptedScaledTemporal(var8, var9, var5, SSType.DATETIMEOFFSET, (short)var11);
         default:
            MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
            Object[] var13 = new Object[]{var2};
            throw new SQLServerException(this, var12.format(var13), null, 0, false);
      }
   }

   private byte[] normalizedValue(JDBCType var1, Object var2, JDBCType var3, int var4, int var5) throws SQLServerException {
      Long var6 = null;
      byte[] var7 = null;

      try {
         switch (var1) {
            case BIT:
               var6 = (Boolean)var2 ? 1L : 0L;
               return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(var6).array();
            case TINYINT:
            case SMALLINT:
               switch (var3) {
                  case BIT:
                     var6 = new Long((Boolean)var2 ? 1L : 0L);
                     break;
                  default:
                     if (var2 instanceof Integer) {
                        int var30 = (Integer)var2;
                        short var36 = (short)var30;
                        var6 = new Long(var36);
                     } else {
                        var6 = new Long(((Short)var2).shortValue());
                     }
               }

               return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(var6).array();
            case INTEGER:
               switch (var3) {
                  case BIT:
                     var6 = new Long((Boolean)var2 ? 1L : 0L);
                     break;
                  case TINYINT:
                  case SMALLINT:
                     var6 = new Long(((Short)var2).shortValue());
                     break;
                  default:
                     var6 = new Long(((Integer)var2).intValue());
               }

               return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(var6).array();
            case BIGINT:
               switch (var3) {
                  case BIT:
                     var6 = new Long((Boolean)var2 ? 1L : 0L);
                     break;
                  case TINYINT:
                  case SMALLINT:
                     var6 = new Long(((Short)var2).shortValue());
                     break;
                  case INTEGER:
                     var6 = new Long(((Integer)var2).intValue());
                     break;
                  default:
                     var6 = new Long((Long)var2);
               }

               return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(var6).array();
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
               B var10 = null;
               if (var2 instanceof String) {
                  var10 = ParameterUtils.HexToBin((String)var2);
               } else {
                  var10 = (byte[])var2;
               }

               if (((Object[])var10).length > var4) {
                  MessageFormat var35 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
                  Object[] var41 = new Object[]{var3, var1};
                  throw new SQLServerException(this, var35.format(var41), null, 0, false);
               }

               return (byte[])var10;
            case GUID:
               return Util.asGuidByteArray(UUID.fromString((String)var2));
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
               if (((String)var2).length() > var4) {
                  MessageFormat var34 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
                  Object[] var40 = new Object[]{var3, var1};
                  throw new SQLServerException(this, var34.format(var40), null, 0, false);
               }

               return ((String)var2).getBytes(Charset.forName("UTF-8"));
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
               if (((String)var2).length() > var4) {
                  MessageFormat var33 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
                  Object[] var39 = new Object[]{var3, var1};
                  throw new SQLServerException(this, var33.format(var39), null, 0, false);
               }

               return ((String)var2).getBytes(Charset.forName("UTF-16LE"));
            case REAL:
            case FLOAT:
               Float var32 = var2 instanceof String ? Float.parseFloat((String)var2) : (Float)var2;
               return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(var32).array();
            case DOUBLE:
               Double var38 = var2 instanceof String ? Double.parseDouble((String)var2) : (Double)var2;
               return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(var38).array();
            case NUMERIC:
            case DECIMAL:
               int var9 = ((BigDecimal)var2).scale();
               int var8 = ((BigDecimal)var2).precision();
               BigDecimal var13 = (BigDecimal)var2;
               if (var8 <= var4 && var9 <= var5) {
                  if (var9 < var5) {
                     var13 = var13.setScale(var5);
                  }

                  var7 = DDC.convertBigDecimalToBytes(var13, var13.scale());
                  byte[] var42 = new byte[16];
                  System.arraycopy(var7, 2, var42, 0, var7.length - 2);
                  return var42;
               }

               MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
               Object[] var43 = new Object[]{var3, var1};
               throw new SQLServerException(this, var14.format(var43), null, 0, false);
            case SMALLMONEY:
            case MONEY:
               BigDecimal var15 = (BigDecimal)var2;
               Util.validateMoneyRange(var15, var1);
               int var16 = var15.precision() - var15.scale() + 4;
               long var17 = ((BigDecimal)var2).multiply(new BigDecimal(10000), new MathContext(var16, RoundingMode.HALF_UP)).longValue();
               ByteBuffer var19 = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
               var19.putInt((int)(var17 >> 32)).array();
               var19.putInt((int)var17).array();
               return var19.array();
            default:
               MessageFormat var20 = new MessageFormat(SQLServerException.getErrString("R_UnsupportedDataTypeAE"));
               Object[] var21 = new Object[]{var1};
               throw new SQLServerException(this, var20.format(var21), null, 0, false);
         }
      } catch (NumberFormatException var22) {
         MessageFormat var31 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
         Object[] var37 = new Object[]{var3, var1};
         throw new SQLServerException(this, var31.format(var37), null, 0, false);
      } catch (IllegalArgumentException var23) {
         MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
         Object[] var12 = new Object[]{var3, var1};
         throw new SQLServerException(this, var11.format(var12), null, 0, false);
      }
   }

   private boolean goToNextRow() throws SQLServerException {
      try {
         return null != this.sourceResultSet ? this.sourceResultSet.next() : this.sourceBulkRecord.next();
      } catch (SQLException var2) {
         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var2);
      }
   }

   private final boolean writeBatchData(TDSWriter var1) throws SQLServerException {
      int var2 = this.copyOptions.getBatchSize();

      for (int var3 = 0; 0 == var2 || var3 < var2; var3++) {
         if (!this.goToNextRow()) {
            return false;
         }

         var1.writeByte((byte)-47);
         int var4 = this.columnMappings.size();
         if (null != this.sourceResultSet) {
            for (int var7 = 0; var7 < var4; var7++) {
               this.writeColumn(
                  var1,
                  ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var7)).sourceColumnOrdinal,
                  ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var7)).destinationColumnOrdinal,
                  null
               );
            }
         } else {
            Object[] var5 = this.sourceBulkRecord.getRowData();

            for (int var6 = 0; var6 < var4; var6++) {
               this.writeColumn(
                  var1,
                  ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var6)).sourceColumnOrdinal,
                  ((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var6)).destinationColumnOrdinal,
                  var5[((SQLServerBulkCopy.ColumnMapping)this.columnMappings.get(var6)).sourceColumnOrdinal - 1]
               );
            }
         }
      }

      return true;
   }

   class BulkColumnMetaData {
      String columnName;
      SSType ssType = null;
      int jdbcType;
      int precision;
      int scale;
      SQLCollation collation;
      byte[] flags = new byte[2];
      boolean isIdentity = false;
      boolean isNullable;
      String collationName;
      CryptoMetadata cryptoMeta = null;
      DateTimeFormatter dateTimeFormatter = null;
      String encryptionType = null;

      BulkColumnMetaData(Column var2) throws SQLServerException {
         this.cryptoMeta = var2.getCryptoMetadata();
         TypeInfo var3 = var2.getTypeInfo();
         this.columnName = var2.getColumnName();
         this.ssType = var3.getSSType();
         this.flags = var3.getFlags();
         this.isIdentity = var3.isIdentity();
         this.isNullable = var3.isNullable();
         this.precision = var3.getPrecision();
         this.scale = var3.getScale();
         this.collation = var3.getSQLCollation();
         this.jdbcType = this.ssType.getJDBCType().getIntValue();
      }

      BulkColumnMetaData(String var2, boolean var3, int var4, int var5, int var6, DateTimeFormatter var7) throws SQLServerException {
         this.columnName = var2;
         this.isNullable = var3;
         this.precision = var4;
         this.scale = var5;
         this.jdbcType = var6;
         this.dateTimeFormatter = var7;
      }

      BulkColumnMetaData(Column var2, String var3, String var4) throws SQLServerException {
         this(var2);
         this.collationName = var3;
         this.encryptionType = var4;
      }

      BulkColumnMetaData(SQLServerBulkCopy.BulkColumnMetaData var2, CryptoMetadata var3) {
         this.columnName = var2.columnName;
         this.isNullable = var2.isNullable;
         this.precision = var2.precision;
         this.scale = var2.scale;
         this.jdbcType = var2.jdbcType;
         this.cryptoMeta = var3;
      }
   }

   private final class BulkTimeoutTimer implements Runnable {
      private final int timeoutSeconds;
      private int secondsRemaining;
      private final TDSCommand command;
      private Thread timerThread;
      private volatile boolean canceled = false;

      BulkTimeoutTimer(int var2, TDSCommand var3) {
         assert var2 > 0;
         assert null != var3;
         this.timeoutSeconds = var2;
         this.secondsRemaining = var2;
         this.command = var3;
      }

      final void start() {
         this.timerThread = new Thread(this);
         this.timerThread.setDaemon(true);
         this.timerThread.start();
      }

      final void stop() {
         this.canceled = true;
         this.timerThread.interrupt();
      }

      final boolean expired() {
         return this.secondsRemaining <= 0;
      }

      public void run() {
         try {
            do {
               if (this.canceled) {
                  return;
               }

               Thread.sleep(1000L);
            } while (--this.secondsRemaining > 0);
         } catch (InterruptedException var3) {
            return;
         }

         try {
            this.command.interrupt(SQLServerException.getErrString("R_queryTimedOut"));
         } catch (SQLServerException var2) {
            this.command.log(Level.FINE, "Command could not be timed out. Reason: " + var2.getMessage());
         }
      }
   }

   private class ColumnMapping {
      String sourceColumnName = null;
      int sourceColumnOrdinal = -1;
      String destinationColumnName = null;
      int destinationColumnOrdinal = -1;

      ColumnMapping(String var2, String var3) {
         this.sourceColumnName = var2;
         this.destinationColumnName = var3;
      }

      ColumnMapping(String var2, int var3) {
         this.sourceColumnName = var2;
         this.destinationColumnOrdinal = var3;
      }

      ColumnMapping(int var2, String var3) {
         this.sourceColumnOrdinal = var2;
         this.destinationColumnName = var3;
      }

      ColumnMapping(int var2, int var3) {
         this.sourceColumnOrdinal = var2;
         this.destinationColumnOrdinal = var3;
      }
   }
}
