package com.microsoft.sqlserver.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import microsoft.sql.DateTimeOffset;

public final class SQLServerResultSet implements ISQLServerResultSet {
   private static int lastResultSetID = 0;
   private final String traceID;
   static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerResultSet");
   private static final Logger loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.ResultSet");
   private final String loggingClassName;
   private final SQLServerStatement stmt;
   private final int maxRows;
   private ResultSetMetaData metaData;
   private boolean isClosed = false;
   private final int serverCursorId;
   private int fetchDirection;
   private int fetchSize;
   private boolean isOnInsertRow = false;
   private boolean lastValueWasNull = false;
   private int lastColumnIndex;
   private boolean areNullCompressedColumnsInitialized = false;
   private RowType resultSetCurrentRowType = RowType.UNKNOWN;
   private Closeable activeStream;
   private final ScrollWindow scrollWindow;
   private static final int BEFORE_FIRST_ROW = 0;
   private static final int AFTER_LAST_ROW = -1;
   private static final int UNKNOWN_ROW = -2;
   private int currentRow = 0;
   private boolean updatedCurrentRow = false;
   private boolean deletedCurrentRow = false;
   static final int UNKNOWN_ROW_COUNT = -3;
   private int rowCount;
   private final Column[] columns;
   private CekTable cekTable = null;
   private TDSReader tdsReader;
   private final SQLServerResultSet.FetchBuffer fetchBuffer;
   private SQLServerException rowErrorException = null;
   private int numFetchedRows;

   private static synchronized int nextResultSetID() {
      return ++lastResultSetID;
   }

   public String toString() {
      return this.traceID;
   }

   String logCursorState() {
      return " currentRow:" + this.currentRow + " numFetchedRows:" + this.numFetchedRows + " rowCount:" + this.rowCount;
   }

   String getClassNameLogging() {
      return this.loggingClassName;
   }

   final RowType getCurrentRowType() {
      return this.resultSetCurrentRowType;
   }

   final void setCurrentRowType(RowType var1) {
      this.resultSetCurrentRowType = var1;
   }

   final boolean getUpdatedCurrentRow() {
      return this.updatedCurrentRow;
   }

   final void setUpdatedCurrentRow(boolean var1) {
      this.updatedCurrentRow = var1;
   }

   final boolean getDeletedCurrentRow() {
      return this.deletedCurrentRow;
   }

   final void setDeletedCurrentRow(boolean var1) {
      this.deletedCurrentRow = var1;
   }

   CekTable getCekTable() {
      return this.cekTable;
   }

   final void setColumnName(int var1, String var2) {
      this.columns[var1 - 1].setColumnName(var2);
   }

   private final void skipColumns(int var1, boolean var2) throws SQLServerException {
      assert this.lastColumnIndex >= 1;
      assert 0 <= var1 && var1 <= this.columns.length;

      for (int var3 = 0; var3 < var1; var3++) {
         Column var4 = this.getColumn(this.lastColumnIndex++);
         var4.skipValue(this.tdsReader, var2 && this.isForwardOnly());
         if (var2) {
            var4.clear();
         }
      }
   }

   SQLServerResultSet(SQLServerStatement var1) throws SQLServerException {
      int var2 = nextResultSetID();
      this.loggingClassName = "com.microsoft.sqlserver.jdbc.SQLServerResultSet:" + var2;
      this.traceID = "SQLServerResultSet:" + var2;
      this.stmt = var1;
      this.maxRows = var1.maxRows;
      this.fetchSize = var1.nFetchSize;
      this.fetchDirection = var1.nFetchDirection;

      final class ClientCursorInitializer extends CursorInitializer {
         private int rowCount = -3;

         @Override
         final int getRowCount() {
            return this.rowCount;
         }

         @Override
         final int getServerCursorId() {
            return 0;
         }

         ClientCursorInitializer() {
            super("ClientCursorInitializer");
         }

         boolean onRow(TDSReader var1) throws SQLServerException {
            return false;
         }

         boolean onNBCRow(TDSReader var1) throws SQLServerException {
            return false;
         }

         boolean onError(TDSReader var1) throws SQLServerException {
            this.rowCount = 0;
            return false;
         }

         boolean onDone(TDSReader var1) throws SQLServerException {
            this.rowCount = 0;
            return false;
         }
      }


      abstract class CursorInitializer extends TDSTokenHandler {
         private StreamColumns columnMetaData = null;
         private StreamColInfo colInfo = null;
         private StreamTabName tabName = null;

         abstract int getRowCount();

         abstract int getServerCursorId();

         final Column[] buildColumns() throws SQLServerException {
            return this.columnMetaData.buildColumns(this.colInfo, this.tabName);
         }

         CursorInitializer(String var2) {
            super(var2);
         }

         boolean onColInfo(TDSReader var1) throws SQLServerException {
            this.colInfo = new StreamColInfo();
            this.colInfo.setFromTDS(var1);
            return true;
         }

         boolean onTabName(TDSReader var1) throws SQLServerException {
            this.tabName = new StreamTabName();
            this.tabName.setFromTDS(var1);
            return true;
         }

         boolean onColMetaData(TDSReader var1) throws SQLServerException {
            this.columnMetaData = new StreamColumns(
               Util.shouldHonorAEForRead(SQLServerResultSet.this.stmt.stmtColumnEncriptionSetting, SQLServerResultSet.this.stmt.connection)
            );
            this.columnMetaData.setFromTDS(var1);
            SQLServerResultSet.this.cekTable = this.columnMetaData.getCekTable();
            return true;
         }
      }


      final class ServerCursorInitializer extends CursorInitializer {
         private final SQLServerStatement stmt;

         @Override
         final int getRowCount() {
            return this.stmt.getServerCursorRowCount();
         }

         @Override
         final int getServerCursorId() {
            return this.stmt.getServerCursorId();
         }

         ServerCursorInitializer(SQLServerStatement var2) {
            super("ServerCursorInitializer");
            this.stmt = var2;
         }

         boolean onRetStatus(TDSReader var1) throws SQLServerException {
            this.stmt.consumeExecOutParam(var1);
            return true;
         }

         boolean onRetValue(TDSReader var1) throws SQLServerException {
            return false;
         }
      }

      CursorInitializer var3 = var1.executedSqlDirectly ? new ClientCursorInitializer() : new ServerCursorInitializer(var1);
      TDSParser.parse(var1.resultsReader(), var3);
      this.columns = var3.buildColumns();
      this.rowCount = var3.getRowCount();
      this.serverCursorId = var3.getServerCursorId();
      this.tdsReader = 0 == this.serverCursorId ? var1.resultsReader() : null;
      this.fetchBuffer = new SQLServerResultSet.FetchBuffer();
      this.scrollWindow = this.isForwardOnly() ? null : new ScrollWindow(this.fetchSize);
      this.numFetchedRows = 0;
      var1.incrResultSetCount();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " created by (" + this.stmt.toString() + ")");
      }
   }

   public boolean isWrapperFor(Class<?> var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "isWrapperFor");
      DriverJDBCVersion.checkSupportsJDBC4();
      boolean var2 = var1.isInstance(this);
      loggerExternal.exiting(this.getClassNameLogging(), "isWrapperFor", var2);
      return var2;
   }

   public <T> T unwrap(Class<T> var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "unwrap");
      DriverJDBCVersion.checkSupportsJDBC4();

      Object var2;
      try {
         var2 = var1.cast(this);
      } catch (ClassCastException var4) {
         throw new SQLServerException(var4.getMessage(), var4);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "unwrap", var2);
      return (T)var2;
   }

   void checkClosed() throws SQLServerException {
      if (this.isClosed) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_resultsetClosed"), null, false);
      }

      this.stmt.checkClosed();
      if (null != this.rowErrorException) {
         throw this.rowErrorException;
      }
   }

   public boolean isClosed() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "isClosed");
      boolean var1 = this.isClosed || this.stmt.isClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "isClosed", var1);
      return var1;
   }

   private final void throwNotScrollable() throws SQLServerException {
      SQLServerException.makeFromDriverError(this.stmt.connection, this, SQLServerException.getErrString("R_requestedOpNotSupportedOnForward"), null, true);
   }

   private final boolean isForwardOnly() {
      return 2003 == this.stmt.getSQLResultSetType() || 2004 == this.stmt.getSQLResultSetType();
   }

   private final boolean isDynamic() {
      return 0 != this.serverCursorId && 2 == this.stmt.getCursorType();
   }

   private final void verifyResultSetIsScrollable() throws SQLServerException {
      if (this.isForwardOnly()) {
         this.throwNotScrollable();
      }
   }

   private final void throwNotUpdatable() throws SQLServerException {
      SQLServerException.makeFromDriverError(this.stmt.connection, this, SQLServerException.getErrString("R_resultsetNotUpdatable"), null, true);
   }

   private final void verifyResultSetIsUpdatable() throws SQLServerException {
      if (1007 == this.stmt.resultSetConcurrency || 0 == this.serverCursorId) {
         this.throwNotUpdatable();
      }
   }

   private boolean hasCurrentRow() {
      return 0 != this.currentRow && -1 != this.currentRow;
   }

   private void verifyResultSetHasCurrentRow() throws SQLServerException {
      if (!this.hasCurrentRow()) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_resultsetNoCurrentRow"), null, true);
      }
   }

   private void verifyCurrentRowIsNotDeleted(String var1) throws SQLServerException {
      if (this.currentRowDeleted()) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString(var1), null, true);
      }
   }

   private void verifyValidColumnIndex(int var1) throws SQLServerException {
      int var2 = this.columns.length;
      if (0 != this.serverCursorId) {
         var2--;
      }

      if (var1 < 1 || var1 > var2) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_indexOutOfRange"));
         Object[] var4 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, var3.format(var4), "07009", false);
      }
   }

   private void verifyResultSetIsNotOnInsertRow() throws SQLServerException {
      if (this.isOnInsertRow) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_mustNotBeOnInsertRow"), null, true);
      }
   }

   private final void throwUnsupportedCursorOp() throws SQLServerException {
      SQLServerException.makeFromDriverError(this.stmt.connection, this, SQLServerException.getErrString("R_unsupportedCursorOperation"), null, true);
   }

   private void closeInternal() {
      if (!this.isClosed) {
         this.isClosed = true;
         this.discardFetchBuffer();
         this.closeServerCursor();
         this.metaData = null;
         this.stmt.decrResultSetCount();
      }
   }

   public void close() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "close");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.closeInternal();
      loggerExternal.exiting(this.getClassNameLogging(), "close");
   }

   public int findColumn(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "findColumn", var1);
      this.checkClosed();

      for (int var2 = 0; var2 < this.columns.length; var2++) {
         if (this.columns[var2].getColumnName().equals(var1)) {
            loggerExternal.exiting(this.getClassNameLogging(), "findColumn", var2 + 1);
            return var2 + 1;
         }
      }

      for (int var5 = 0; var5 < this.columns.length; var5++) {
         if (this.columns[var5].getColumnName().equalsIgnoreCase(var1)) {
            loggerExternal.exiting(this.getClassNameLogging(), "findColumn", var5 + 1);
            return var5 + 1;
         }
      }

      MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_invalidColumnName"));
      Object[] var4 = new Object[]{var1};
      SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, var3.format(var4), "07009", false);
      return 0;
   }

   final int getColumnCount() {
      int var1 = this.columns.length;
      if (0 != this.serverCursorId) {
         var1--;
      }

      return var1;
   }

   final Column getColumn(int var1) throws SQLServerException {
      if (null != this.activeStream) {
         try {
            this.activeStream.close();
         } catch (IOException var6) {
            SQLServerException.makeFromDriverError(null, null, var6.getMessage(), null, true);
         } finally {
            this.activeStream = null;
         }
      }

      return this.columns[var1 - 1];
   }

   private final void initializeNullCompressedColumns() throws SQLServerException {
      if (this.resultSetCurrentRowType.equals(RowType.NBCROW) && !this.areNullCompressedColumnsInitialized) {
         int var1 = 0;
         int var2 = (this.columns.length - 1 >> 3) + 1;

         for (int var3 = 0; var3 < var2; var3++) {
            int var4 = this.tdsReader.readUnsignedByte();
            if (var4 == 0) {
               var1 += 8;
            } else {
               for (int var5 = 0; var5 < 8 && var1 < this.columns.length; var5++) {
                  if ((var4 & 1 << var5) != 0) {
                     this.columns[var1].initFromCompressedNull();
                  }

                  var1++;
               }
            }
         }

         this.areNullCompressedColumnsInitialized = true;
      }
   }

   private final Column loadColumn(int var1) throws SQLServerException {
      assert 1 <= var1 && var1 <= this.columns.length;
      this.initializeNullCompressedColumns();
      if (var1 > this.lastColumnIndex && !this.columns[var1 - 1].isInitialized()) {
         this.skipColumns(var1 - this.lastColumnIndex, false);
      }

      return this.getColumn(var1);
   }

   private void NotImplemented() throws SQLServerException {
      SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_notSupported"), null, false);
   }

   public void clearWarnings() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "clearWarnings");
      loggerExternal.exiting(this.getClassNameLogging(), "clearWarnings");
   }

   private void moverInit() throws SQLServerException {
      this.cancelInsert();
      this.cancelUpdates();
   }

   public boolean relative(int var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "relative", var1);
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " rows:" + var1 + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.verifyResultSetHasCurrentRow();
      this.moverInit();
      this.moveRelative(var1);
      boolean var2 = this.hasCurrentRow();
      loggerExternal.exiting(this.getClassNameLogging(), "relative", var2);
      return var2;
   }

   private final void moveRelative(int var1) throws SQLServerException {
      assert this.hasCurrentRow();
      if (0 != var1) {
         if (var1 > 0) {
            this.moveForward(var1);
         } else {
            this.moveBackward(var1);
         }
      }
   }

   private final void moveForward(int var1) throws SQLServerException {
      assert this.hasCurrentRow();
      assert var1 > 0;
      if (this.scrollWindow.getRow() + var1 <= this.scrollWindow.getMaxRows()) {
         int var2 = 0;

         while (var1 > 0 && this.scrollWindow.next(this)) {
            var2++;
            var1--;
         }

         this.updateCurrentRow(var2);
         if (0 == var1) {
            return;
         }
      }

      assert var1 > 0;
      if (0 == this.serverCursorId) {
         assert -2 != this.currentRow;
         this.currentRow = this.clientMoveAbsolute(this.currentRow + var1);
      } else {
         if (1 == var1) {
            this.doServerFetch(2, 0, this.fetchSize);
         } else {
            this.doServerFetch(32, var1 + this.scrollWindow.getRow() - 1, this.fetchSize);
         }

         if (!this.scrollWindow.next(this)) {
            this.currentRow = -1;
         } else {
            this.updateCurrentRow(var1);
         }
      }
   }

   private final void moveBackward(int var1) throws SQLServerException {
      assert this.hasCurrentRow();
      assert var1 < 0;
      if (this.scrollWindow.getRow() + var1 >= 1) {
         for (int var2 = 0; var2 > var1; var2--) {
            this.scrollWindow.previous(this);
         }

         this.updateCurrentRow(var1);
      } else if (0 == this.serverCursorId) {
         assert -2 != this.currentRow;
         if (this.currentRow + var1 < 1) {
            this.moveBeforeFirst();
         } else {
            this.currentRow = this.clientMoveAbsolute(this.currentRow + var1);
         }
      } else {
         if (-1 == var1) {
            this.doServerFetch(512, 0, this.fetchSize);
            if (!this.scrollWindow.next(this)) {
               this.currentRow = 0;
               return;
            }

            while (this.scrollWindow.next(this)) {
            }

            this.scrollWindow.previous(this);
         } else {
            this.doServerFetch(32, var1 + this.scrollWindow.getRow() - 1, this.fetchSize);
            if (!this.scrollWindow.next(this)) {
               this.currentRow = 0;
               return;
            }
         }

         this.updateCurrentRow(var1);
      }
   }

   private final void updateCurrentRow(int var1) {
      if (-2 != this.currentRow) {
         assert this.currentRow >= 1;
         this.currentRow += var1;
         assert this.currentRow >= 1;
      }
   }

   public boolean next() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "next");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.moverInit();
      if (-1 == this.currentRow) {
         loggerExternal.exiting(this.getClassNameLogging(), "next", false);
         return false;
      }

      if (!this.isForwardOnly()) {
         if (0 == this.currentRow) {
            this.moveFirst();
         } else {
            this.moveForward(1);
         }

         boolean var1 = this.hasCurrentRow();
         loggerExternal.exiting(this.getClassNameLogging(), "next", var1);
         return var1;
      } else {
         if (0 != this.serverCursorId && this.maxRows > 0 && this.currentRow == this.maxRows) {
            this.currentRow = -1;
            loggerExternal.exiting(this.getClassNameLogging(), "next", false);
            return false;
         }

         if (this.fetchBufferNext()) {
            if (0 == this.currentRow) {
               this.currentRow = 1;
            } else {
               this.updateCurrentRow(1);
            }

            assert 0 == this.maxRows || this.currentRow <= this.maxRows;
            loggerExternal.exiting(this.getClassNameLogging(), "next", true);
            return true;
         } else {
            if (0 != this.serverCursorId) {
               this.doServerFetch(2, 0, this.fetchSize);
               if (this.fetchBufferNext()) {
                  if (0 == this.currentRow) {
                     this.currentRow = 1;
                  } else {
                     this.updateCurrentRow(1);
                  }

                  assert 0 == this.maxRows || this.currentRow <= this.maxRows;
                  loggerExternal.exiting(this.getClassNameLogging(), "next", true);
                  return true;
               }
            }

            if (-3 == this.rowCount) {
               this.rowCount = this.currentRow;
            }

            this.currentRow = -1;
            loggerExternal.exiting(this.getClassNameLogging(), "next", false);
            return false;
         }
      }
   }

   public boolean wasNull() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "wasNull");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "wasNull", this.lastValueWasNull);
      return this.lastValueWasNull;
   }

   public boolean isBeforeFirst() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isBeforeFirst");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      if (0 != this.serverCursorId) {
         switch (this.stmt.getCursorType()) {
            case 2:
               this.throwUnsupportedCursorOp();
               break;
            case 4:
               this.throwNotScrollable();
               break;
            case 16:
               this.throwNotScrollable();
         }
      }

      if (this.isOnInsertRow) {
         return false;
      }

      if (0 != this.currentRow) {
         return false;
      }

      if (0 == this.serverCursorId) {
         return this.fetchBufferHasRows();
      }

      assert this.rowCount >= 0;
      boolean var1 = this.rowCount > 0;
      loggerExternal.exiting(this.getClassNameLogging(), "isBeforeFirst", var1);
      return var1;
   }

   public boolean isAfterLast() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isAfterLast");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      if (0 != this.serverCursorId) {
         this.verifyResultSetIsScrollable();
         if (2 == this.stmt.getCursorType() && !this.isForwardOnly()) {
            this.throwUnsupportedCursorOp();
         }
      }

      if (this.isOnInsertRow) {
         return false;
      }

      assert -1 != this.currentRow || -3 != this.rowCount;
      boolean var1 = -1 == this.currentRow && this.rowCount > 0;
      loggerExternal.exiting(this.getClassNameLogging(), "isAfterLast", var1);
      return var1;
   }

   public boolean isFirst() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isFirst");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      if (this.isDynamic()) {
         this.throwUnsupportedCursorOp();
      }

      if (this.isOnInsertRow) {
         return false;
      }

      assert -2 != this.currentRow;
      boolean var1 = 1 == this.currentRow;
      loggerExternal.exiting(this.getClassNameLogging(), "isFirst", var1);
      return var1;
   }

   public boolean isLast() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isLast");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      if (this.isDynamic()) {
         this.throwUnsupportedCursorOp();
      }

      if (this.isOnInsertRow) {
         return false;
      } else if (!this.hasCurrentRow()) {
         return false;
      } else {
         assert this.currentRow >= 1;
         if (-3 != this.rowCount) {
            assert this.currentRow <= this.rowCount;
            return this.currentRow == this.rowCount;
         } else {
            assert 0 == this.serverCursorId;
            boolean var1 = !this.next();
            this.previous();
            loggerExternal.exiting(this.getClassNameLogging(), "isLast", var1);
            return var1;
         }
      }
   }

   public void beforeFirst() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "beforeFirst");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.moverInit();
      this.moveBeforeFirst();
      loggerExternal.exiting(this.getClassNameLogging(), "beforeFirst");
   }

   private final void moveBeforeFirst() throws SQLServerException {
      if (0 == this.serverCursorId) {
         this.fetchBufferBeforeFirst();
         this.scrollWindow.clear();
      } else {
         this.doServerFetch(1, 0, 0);
      }

      this.currentRow = 0;
   }

   public void afterLast() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "afterLast");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.moverInit();
      this.moveAfterLast();
      loggerExternal.exiting(this.getClassNameLogging(), "afterLast");
   }

   private void moveAfterLast() throws SQLServerException {
      assert !this.isForwardOnly();
      if (0 == this.serverCursorId) {
         this.clientMoveAfterLast();
      } else {
         this.doServerFetch(8, 0, 0);
      }

      this.currentRow = -1;
   }

   public boolean first() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "first");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.moverInit();
      this.moveFirst();
      boolean var1 = this.hasCurrentRow();
      loggerExternal.exiting(this.getClassNameLogging(), "first", var1);
      return var1;
   }

   private final void moveFirst() throws SQLServerException {
      if (0 == this.serverCursorId) {
         this.moveBeforeFirst();
      } else {
         this.doServerFetch(1, 0, this.fetchSize);
      }

      if (!this.scrollWindow.next(this)) {
         this.currentRow = -1;
      } else {
         this.currentRow = this.isDynamic() ? -2 : 1;
      }
   }

   public boolean last() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "last");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.moverInit();
      this.moveLast();
      boolean var1 = this.hasCurrentRow();
      loggerExternal.exiting(this.getClassNameLogging(), "last", var1);
      return var1;
   }

   private final void moveLast() throws SQLServerException {
      if (0 == this.serverCursorId) {
         this.currentRow = this.clientMoveAbsolute(-1);
      } else {
         this.doServerFetch(8, 0, this.fetchSize);
         if (!this.scrollWindow.next(this)) {
            this.currentRow = -1;
         } else {
            while (this.scrollWindow.next(this)) {
            }

            this.scrollWindow.previous(this);
            this.currentRow = this.isDynamic() ? -2 : this.rowCount;
         }
      }
   }

   public int getRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getRow");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      if (this.isDynamic() && !this.isForwardOnly()) {
         this.throwUnsupportedCursorOp();
      }

      if (!this.hasCurrentRow() || this.isOnInsertRow) {
         return 0;
      }

      assert this.currentRow >= 1;
      loggerExternal.exiting(this.getClassNameLogging(), "getRow", this.currentRow);
      return this.currentRow;
   }

   public boolean absolute(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "absolute");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " row:" + var1 + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      if (this.isDynamic()) {
         this.throwUnsupportedCursorOp();
      }

      this.moverInit();
      this.moveAbsolute(var1);
      boolean var2 = this.hasCurrentRow();
      loggerExternal.exiting(this.getClassNameLogging(), "absolute", var2);
      return var2;
   }

   private void moveAbsolute(int var1) throws SQLServerException {
      assert -2 != this.currentRow;
      assert !this.isDynamic();
      switch (var1) {
         case -1:
            this.moveLast();
            return;
         case 0:
            this.moveBeforeFirst();
            return;
         case 1:
            this.moveFirst();
            return;
         default:
            if (this.hasCurrentRow()) {
               assert this.currentRow >= 1;
               if (var1 > 0) {
                  this.moveRelative(var1 - this.currentRow);
                  return;
               }

               if (-3 != this.rowCount) {
                  assert var1 < 0;
                  this.moveRelative(this.rowCount + var1 + 1 - this.currentRow);
                  return;
               }
            }

            if (0 == this.serverCursorId) {
               this.currentRow = this.clientMoveAbsolute(var1);
            } else {
               this.doServerFetch(16, var1, this.fetchSize);
               if (!this.scrollWindow.next(this)) {
                  this.currentRow = var1 < 0 ? 0 : -1;
               } else {
                  if (var1 > 0) {
                     this.currentRow = var1;
                  } else {
                     assert var1 < 0;
                     assert this.rowCount + var1 + 1 >= 1;
                     this.currentRow = this.rowCount + var1 + 1;
                  }
               }
            }
      }
   }

   private final boolean fetchBufferHasRows() throws SQLServerException {
      assert 0 == this.serverCursorId;
      assert null != this.tdsReader;
      assert this.lastColumnIndex >= 0;
      if (this.lastColumnIndex >= 1) {
         return true;
      }

      int var1 = this.tdsReader.peekTokenType();
      return 209 == var1 || 210 == var1 || 171 == var1 || 170 == var1;
   }

   final void discardCurrentRow() throws SQLServerException {
      assert this.lastColumnIndex >= 0;
      this.updatedCurrentRow = false;
      this.deletedCurrentRow = false;
      if (this.lastColumnIndex >= 1) {
         this.initializeNullCompressedColumns();

         for (int var1 = 1; var1 < this.lastColumnIndex; var1++) {
            this.getColumn(var1).clear();
         }

         this.skipColumns(this.columns.length + 1 - this.lastColumnIndex, true);
      }

      this.resultSetCurrentRowType = RowType.UNKNOWN;
      this.areNullCompressedColumnsInitialized = false;
   }

   final int fetchBufferGetRow() {
      return this.isForwardOnly() ? this.numFetchedRows : this.scrollWindow.getRow();
   }

   final void fetchBufferBeforeFirst() throws SQLServerException {
      assert 0 == this.serverCursorId;
      assert null != this.tdsReader;
      this.discardCurrentRow();
      this.fetchBuffer.reset();
      this.lastColumnIndex = 0;
   }

   final TDSReaderMark fetchBufferMark() {
      assert null != this.tdsReader;
      return this.tdsReader.mark();
   }

   final void fetchBufferReset(TDSReaderMark var1) throws SQLServerException {
      assert null != this.tdsReader;
      assert null != var1;
      this.discardCurrentRow();
      this.tdsReader.reset(var1);
      this.lastColumnIndex = 1;
   }

   final boolean fetchBufferNext() throws SQLServerException {
      if (null == this.tdsReader) {
         return false;
      }

      this.discardCurrentRow();
      RowType var1 = RowType.UNKNOWN;

      try {
         var1 = this.fetchBuffer.nextRow();
         if (var1.equals(RowType.UNKNOWN)) {
            return false;
         }
      } catch (SQLServerException var6) {
         this.currentRow = -1;
         this.rowErrorException = var6;
         throw var6;
      } finally {
         this.lastColumnIndex = 0;
         this.resultSetCurrentRowType = var1;
      }

      this.numFetchedRows++;
      this.lastColumnIndex = 1;
      return true;
   }

   private final void clientMoveAfterLast() throws SQLServerException {
      assert -2 != this.currentRow;
      int var1 = 0;

      while (this.fetchBufferNext()) {
         var1++;
      }

      if (-3 == this.rowCount) {
         assert -1 != this.currentRow;
         this.rowCount = (0 == this.currentRow ? 0 : this.currentRow) + var1;
      }
   }

   private final int clientMoveAbsolute(int var1) throws SQLServerException {
      assert 0 == this.serverCursorId;
      this.scrollWindow.clear();
      if (var1 < 0) {
         if (-3 == this.rowCount) {
            this.clientMoveAfterLast();
            this.currentRow = -1;
         }

         assert this.rowCount >= 0;
         if (this.rowCount + var1 < 0) {
            this.moveBeforeFirst();
            return 0;
         }

         var1 = this.rowCount + var1 + 1;
      }

      assert var1 > 0;
      if (-1 == this.currentRow || var1 <= this.currentRow) {
         this.moveBeforeFirst();
      }

      assert 0 == this.currentRow || this.currentRow < var1;

      while (this.currentRow != var1) {
         if (!this.fetchBufferNext()) {
            if (-3 == this.rowCount) {
               this.rowCount = this.currentRow;
            }

            return -1;
         }

         if (0 == this.currentRow) {
            this.currentRow = 1;
         } else {
            this.updateCurrentRow(1);
         }
      }

      return var1;
   }

   public boolean previous() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "previous");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.moverInit();
      if (0 == this.currentRow) {
         return false;
      }

      if (-1 == this.currentRow) {
         this.moveLast();
      } else {
         this.moveBackward(-1);
      }

      boolean var1 = this.hasCurrentRow();
      loggerExternal.exiting(this.getClassNameLogging(), "previous", var1);
      return var1;
   }

   private final void cancelInsert() {
      if (this.isOnInsertRow) {
         this.isOnInsertRow = false;
         this.clearColumnsValues();
      }
   }

   final void clearColumnsValues() {
      int var1 = this.columns.length;

      for (int var2 = 0; var2 < var1; var2++) {
         this.columns[var2].cancelUpdates();
      }
   }

   public SQLWarning getWarnings() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getWarnings");
      loggerExternal.exiting(this.getClassNameLogging(), "getWarnings", null);
      return null;
   }

   public void setFetchDirection(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setFetchDirection", var1);
      this.checkClosed();
      this.verifyResultSetIsScrollable();
      if (1000 != var1 && 1001 != var1 && 1002 != var1 || 1000 != var1 && (2003 == this.stmt.resultSetType || 2004 == this.stmt.resultSetType)) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidFetchDirection"));
         Object[] var3 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, var2.format(var3), null, false);
      }

      this.fetchDirection = var1;
      loggerExternal.exiting(this.getClassNameLogging(), "setFetchDirection");
   }

   public int getFetchDirection() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFetchDirection");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getFetchDirection", this.fetchDirection);
      return this.fetchDirection;
   }

   public void setFetchSize(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setFetchSize", var1);
      this.checkClosed();
      if (var1 < 0) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_invalidFetchSize"), null, false);
      }

      this.fetchSize = 0 == var1 ? this.stmt.defaultFetchSize : var1;
      loggerExternal.exiting(this.getClassNameLogging(), "setFetchSize");
   }

   public int getFetchSize() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFetchSize");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getFloat", this.fetchSize);
      return this.fetchSize;
   }

   public int getType() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getType");
      this.checkClosed();
      int var1 = this.stmt.getResultSetType();
      loggerExternal.exiting(this.getClassNameLogging(), "getType", var1);
      return var1;
   }

   public int getConcurrency() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getConcurrency");
      this.checkClosed();
      int var1 = this.stmt.getResultSetConcurrency();
      loggerExternal.exiting(this.getClassNameLogging(), "getConcurrency", var1);
      return var1;
   }

   Column getterGetColumn(int var1) throws SQLServerException {
      this.verifyResultSetHasCurrentRow();
      this.verifyCurrentRowIsNotDeleted("R_cantGetColumnValueFromDeletedRow");
      this.verifyValidColumnIndex(var1);
      if (this.updatedCurrentRow) {
         this.doRefreshRow();
         this.verifyResultSetHasCurrentRow();
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " Getting Column:" + var1);
      }

      return this.loadColumn(var1);
   }

   private final Object getValue(int var1, JDBCType var2) throws SQLServerException {
      return this.getValue(var1, var2, null, null);
   }

   private final Object getValue(int var1, JDBCType var2, Calendar var3) throws SQLServerException {
      return this.getValue(var1, var2, null, var3);
   }

   private final Object getValue(int var1, JDBCType var2, InputStreamGetterArgs var3) throws SQLServerException {
      return this.getValue(var1, var2, var3, null);
   }

   private final Object getValue(int var1, JDBCType var2, InputStreamGetterArgs var3, Calendar var4) throws SQLServerException {
      Object var5 = this.getterGetColumn(var1).getValue(var2, var3, var4, this.tdsReader);
      this.lastValueWasNull = null == var5;
      return var5;
   }

   private Object getStream(int var1, StreamType var2) throws SQLServerException {
      Object var3 = this.getValue(
         var1,
         var2.getJDBCType(),
         new InputStreamGetterArgs(var2, this.stmt.getExecProps().isResponseBufferingAdaptive(), this.isForwardOnly(), this.toString())
      );
      this.activeStream = (Closeable)var3;
      return var3;
   }

   private SQLXML getSQLXMLInternal(int var1) throws SQLServerException {
      SQLServerSQLXML var2 = (SQLServerSQLXML)this.getValue(
         var1,
         JDBCType.SQLXML,
         new InputStreamGetterArgs(StreamType.SQLXML, this.stmt.getExecProps().isResponseBufferingAdaptive(), this.isForwardOnly(), this.toString())
      );
      if (null != var2) {
         this.activeStream = var2.getStream();
      }

      return var2;
   }

   public InputStream getAsciiStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getAsciiStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(var1, StreamType.ASCII);
      loggerExternal.exiting(this.getClassNameLogging(), "getAsciiStream", var2);
      return var2;
   }

   public InputStream getAsciiStream(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getAsciiStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(this.findColumn(var1), StreamType.ASCII);
      loggerExternal.exiting(this.getClassNameLogging(), "getAsciiStream", var2);
      return var2;
   }

   @Deprecated
   public BigDecimal getBigDecimal(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      BigDecimal var3 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      if (null != var3) {
         var3 = var3.setScale(var2, 1);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var3);
      return var3;
   }

   @Deprecated
   public BigDecimal getBigDecimal(String var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "columnName", new Object[]{var1, var2});
      }

      this.checkClosed();
      BigDecimal var3 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      if (null != var3) {
         var3 = var3.setScale(var2, 1);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var3);
      return var3;
   }

   public InputStream getBinaryStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBinaryStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(var1, StreamType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBinaryStream", var2);
      return var2;
   }

   public InputStream getBinaryStream(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBinaryStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(this.findColumn(var1), StreamType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBinaryStream", var2);
      return var2;
   }

   public boolean getBoolean(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBoolean", var1);
      this.checkClosed();
      Boolean var2 = (Boolean)this.getValue(var1, JDBCType.BIT);
      loggerExternal.exiting(this.getClassNameLogging(), "getBoolean", var2);
      return null != var2 ? var2 : false;
   }

   public boolean getBoolean(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBoolean", var1);
      this.checkClosed();
      Boolean var2 = (Boolean)this.getValue(this.findColumn(var1), JDBCType.BIT);
      loggerExternal.exiting(this.getClassNameLogging(), "getBoolean", var2);
      return null != var2 ? var2 : false;
   }

   public byte getByte(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getByte", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(var1, JDBCType.TINYINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getByte", var2);
      return null != var2 ? var2.byteValue() : 0;
   }

   public byte getByte(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getByte", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(this.findColumn(var1), JDBCType.TINYINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getByte", var2);
      return null != var2 ? var2.byteValue() : 0;
   }

   public byte[] getBytes(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBytes", var1);
      this.checkClosed();
      byte[] var2 = (byte[])this.getValue(var1, JDBCType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBytes", var2);
      return var2;
   }

   public byte[] getBytes(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBytes", var1);
      this.checkClosed();
      byte[] var2 = (byte[])this.getValue(this.findColumn(var1), JDBCType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBytes", var2);
      return var2;
   }

   public Date getDate(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDate", var1);
      this.checkClosed();
      Date var2 = (Date)this.getValue(var1, JDBCType.DATE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var2);
      return var2;
   }

   public Date getDate(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDate", var1);
      this.checkClosed();
      Date var2 = (Date)this.getValue(this.findColumn(var1), JDBCType.DATE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var2);
      return var2;
   }

   public Date getDate(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      Date var3 = (Date)this.getValue(var1, JDBCType.DATE, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var3);
      return var3;
   }

   public Date getDate(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      Date var3 = (Date)this.getValue(this.findColumn(var1), JDBCType.DATE, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var3);
      return var3;
   }

   public double getDouble(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDouble", var1);
      this.checkClosed();
      Double var2 = (Double)this.getValue(var1, JDBCType.DOUBLE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDouble", var2);
      return null != var2 ? var2 : 0.0;
   }

   public double getDouble(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDouble", var1);
      this.checkClosed();
      Double var2 = (Double)this.getValue(this.findColumn(var1), JDBCType.DOUBLE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDouble", var2);
      return null != var2 ? var2 : 0.0;
   }

   public float getFloat(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFloat", var1);
      this.checkClosed();
      Float var2 = (Float)this.getValue(var1, JDBCType.REAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getFloat", var2);
      return null != var2 ? var2 : 0.0F;
   }

   public float getFloat(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFloat", var1);
      this.checkClosed();
      Float var2 = (Float)this.getValue(this.findColumn(var1), JDBCType.REAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getFloat", var2);
      return null != var2 ? var2 : 0.0F;
   }

   public int getInt(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getInt", var1);
      this.checkClosed();
      Integer var2 = (Integer)this.getValue(var1, JDBCType.INTEGER);
      loggerExternal.exiting(this.getClassNameLogging(), "getInt", var2);
      return null != var2 ? var2 : 0;
   }

   public int getInt(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getInt", var1);
      this.checkClosed();
      Integer var2 = (Integer)this.getValue(this.findColumn(var1), JDBCType.INTEGER);
      loggerExternal.exiting(this.getClassNameLogging(), "getInt", var2);
      return null != var2 ? var2 : 0;
   }

   public long getLong(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getLong", var1);
      this.checkClosed();
      Long var2 = (Long)this.getValue(var1, JDBCType.BIGINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getLong", var2);
      return null != var2 ? var2 : 0L;
   }

   public long getLong(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getLong", var1);
      this.checkClosed();
      Long var2 = (Long)this.getValue(this.findColumn(var1), JDBCType.BIGINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getLong", var2);
      return null != var2 ? var2 : 0L;
   }

   public ResultSetMetaData getMetaData() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMetaData");
      this.checkClosed();
      if (this.metaData == null) {
         this.metaData = new SQLServerResultSetMetaData(this.stmt.connection, this);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getMetaData", this.metaData);
      return this.metaData;
   }

   public Object getObject(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getObject", var1);
      this.checkClosed();
      Object var2 = this.getValue(var1, this.getterGetColumn(var1).getTypeInfo().getSSType().getJDBCType());
      loggerExternal.exiting(this.getClassNameLogging(), "getObject", var2);
      return var2;
   }

   public <T> T getObject(int var1, Class<T> var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public Object getObject(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getObject", var1);
      this.checkClosed();
      Object var2 = this.getObject(this.findColumn(var1));
      loggerExternal.exiting(this.getClassNameLogging(), "getObject", var2);
      return var2;
   }

   public <T> T getObject(String var1, Class<T> var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public short getShort(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getShort", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(var1, JDBCType.SMALLINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getShort", var2);
      return null != var2 ? var2 : 0;
   }

   public short getShort(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getShort", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(this.findColumn(var1), JDBCType.SMALLINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getShort", var2);
      return null != var2 ? var2 : 0;
   }

   public String getString(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getString", var1);
      this.checkClosed();
      String var2 = null;
      Object var3 = this.getValue(var1, JDBCType.CHAR);
      if (null != var3) {
         var2 = var3.toString();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getString", var2);
      return var2;
   }

   public String getString(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getString", var1);
      this.checkClosed();
      String var2 = null;
      Object var3 = this.getValue(this.findColumn(var1), JDBCType.CHAR);
      if (null != var3) {
         var2 = var3.toString();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getString", var2);
      return var2;
   }

   public String getNString(int var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getNString", var1);
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      String var2 = (String)this.getValue(var1, JDBCType.NCHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getNString", var2);
      return var2;
   }

   public String getNString(String var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getNString", var1);
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      String var2 = (String)this.getValue(this.findColumn(var1), JDBCType.NCHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getNString", var2);
      return var2;
   }

   public String getUniqueIdentifier(int var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getUniqueIdentifier", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(var1, JDBCType.GUID);
      loggerExternal.exiting(this.getClassNameLogging(), "getUniqueIdentifier", var2);
      return var2;
   }

   public String getUniqueIdentifier(String var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getUniqueIdentifier", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(this.findColumn(var1), JDBCType.GUID);
      loggerExternal.exiting(this.getClassNameLogging(), "getUniqueIdentifier", var2);
      return var2;
   }

   public Time getTime(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTime", var1);
      this.checkClosed();
      Time var2 = (Time)this.getValue(var1, JDBCType.TIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var2);
      return var2;
   }

   public Time getTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTime", var1);
      this.checkClosed();
      Time var2 = (Time)this.getValue(this.findColumn(var1), JDBCType.TIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var2);
      return var2;
   }

   public Time getTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Time var3 = (Time)this.getValue(var1, JDBCType.TIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var3);
      return var3;
   }

   public Time getTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Time var3 = (Time)this.getValue(this.findColumn(var1), JDBCType.TIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var3);
      return var3;
   }

   public Timestamp getTimestamp(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var2);
      return var2;
   }

   public Timestamp getTimestamp(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var2);
      return var2;
   }

   public Timestamp getTimestamp(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimeStamp", var3);
      return var3;
   }

   public Timestamp getTimestamp(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var3);
      return var3;
   }

   public Timestamp getDateTime(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var2);
      return var2;
   }

   public Timestamp getDateTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var2);
      return var2;
   }

   public Timestamp getDateTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var3);
      return var3;
   }

   public Timestamp getDateTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var3);
      return var3;
   }

   public Timestamp getSmallDateTime(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var2);
      return var2;
   }

   public Timestamp getSmallDateTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var2);
      return var2;
   }

   public Timestamp getSmallDateTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var3);
      return var3;
   }

   public Timestamp getSmallDateTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var3);
      return var3;
   }

   public DateTimeOffset getDateTimeOffset(int var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTimeOffset", var1);
      this.checkClosed();
      if (!this.stmt.connection.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      DateTimeOffset var2 = (DateTimeOffset)this.getValue(var1, JDBCType.DATETIMEOFFSET);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTimeOffset", var2);
      return var2;
   }

   public DateTimeOffset getDateTimeOffset(String var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTimeOffset", var1);
      this.checkClosed();
      if (!this.stmt.connection.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      DateTimeOffset var2 = (DateTimeOffset)this.getValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTimeOffset", var2);
      return var2;
   }

   @Deprecated
   public InputStream getUnicodeStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getUnicodeStream", var1);
      this.NotImplemented();
      return null;
   }

   @Deprecated
   public InputStream getUnicodeStream(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getUnicodeStream", var1);
      this.NotImplemented();
      return null;
   }

   public Object getObject(int var1, Map<String, Class<?>> var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getObject", new Object[]{var1, var2});
      }

      this.NotImplemented();
      return null;
   }

   public Ref getRef(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getRef");
      this.NotImplemented();
      return null;
   }

   public Blob getBlob(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBlob", var1);
      this.checkClosed();
      Blob var2 = (Blob)this.getValue(var1, JDBCType.BLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getBlob", var2);
      return var2;
   }

   public Blob getBlob(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBlob", var1);
      this.checkClosed();
      Blob var2 = (Blob)this.getValue(this.findColumn(var1), JDBCType.BLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getBlob", var2);
      return var2;
   }

   public Clob getClob(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getClob", var1);
      this.checkClosed();
      Clob var2 = (Clob)this.getValue(var1, JDBCType.CLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getClob", var2);
      return var2;
   }

   public Clob getClob(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getClob", var1);
      this.checkClosed();
      Clob var2 = (Clob)this.getValue(this.findColumn(var1), JDBCType.CLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getClob", var2);
      return var2;
   }

   public NClob getNClob(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNClob", var1);
      this.checkClosed();
      NClob var2 = (NClob)this.getValue(var1, JDBCType.NCLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getNClob", var2);
      return var2;
   }

   public NClob getNClob(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNClob", var1);
      this.checkClosed();
      NClob var2 = (NClob)this.getValue(this.findColumn(var1), JDBCType.NCLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getNClob", var2);
      return var2;
   }

   public Array getArray(int var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Object getObject(String var1, Map<String, Class<?>> var2) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Ref getRef(String var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Array getArray(String var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public String getCursorName() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getCursorName");
      SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_positionedUpdatesNotSupported"), null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "getCursorName", null);
      return null;
   }

   public Reader getCharacterStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(var1, StreamType.CHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getCharacterStream", var2);
      return var2;
   }

   public Reader getCharacterStream(String var1) throws SQLServerException {
      this.checkClosed();
      loggerExternal.entering(this.getClassNameLogging(), "getCharacterStream", var1);
      Reader var2 = (Reader)this.getStream(this.findColumn(var1), StreamType.CHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getCharacterStream", var2);
      return var2;
   }

   public Reader getNCharacterStream(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(var1, StreamType.NCHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getNCharacterStream", var2);
      return var2;
   }

   public Reader getNCharacterStream(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(this.findColumn(var1), StreamType.NCHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getNCharacterStream", var2);
      return var2;
   }

   public BigDecimal getBigDecimal(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var2);
      return var2;
   }

   public BigDecimal getBigDecimal(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var2);
      return var2;
   }

   public BigDecimal getMoney(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getMoney", var2);
      return var2;
   }

   public BigDecimal getMoney(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getMoney", var2);
      return var2;
   }

   public BigDecimal getSmallMoney(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallMoney", var2);
      return var2;
   }

   public BigDecimal getSmallMoney(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallMoney", var2);
      return var2;
   }

   public RowId getRowId(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public RowId getRowId(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public SQLXML getSQLXML(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getSQLXML", var1);
      SQLXML var2 = this.getSQLXMLInternal(var1);
      loggerExternal.exiting(this.getClassNameLogging(), "getSQLXML", var2);
      return var2;
   }

   public SQLXML getSQLXML(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getSQLXML", var1);
      SQLXML var2 = this.getSQLXMLInternal(this.findColumn(var1));
      loggerExternal.exiting(this.getClassNameLogging(), "getSQLXML", var2);
      return var2;
   }

   public boolean rowUpdated() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "rowUpdated");
      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      loggerExternal.exiting(this.getClassNameLogging(), "rowUpdated", false);
      return false;
   }

   public boolean rowInserted() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "rowInserted");
      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      loggerExternal.exiting(this.getClassNameLogging(), "rowInserted", false);
      return false;
   }

   public boolean rowDeleted() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "rowDeleted");
      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      if (!this.isOnInsertRow && this.hasCurrentRow()) {
         boolean var1 = this.currentRowDeleted();
         loggerExternal.exiting(this.getClassNameLogging(), "rowDeleted", var1);
         return var1;
      } else {
         return false;
      }
   }

   private final boolean currentRowDeleted() throws SQLServerException {
      assert this.hasCurrentRow();
      assert null != this.tdsReader;
      return this.deletedCurrentRow || 0 != this.serverCursorId && 2 == this.loadColumn(this.columns.length).getInt(this.tdsReader);
   }

   private final Column updaterGetColumn(int var1) throws SQLServerException {
      this.verifyResultSetIsUpdatable();
      this.verifyValidColumnIndex(var1);
      if (!this.columns[var1 - 1].isUpdatable()) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_cantUpdateColumn"), "07009", false);
      }

      if (!this.isOnInsertRow) {
         if (!this.hasCurrentRow()) {
            SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_resultsetNoCurrentRow"), null, true);
         }

         this.verifyCurrentRowIsNotDeleted("R_cantUpdateDeletedRow");
      }

      return this.getColumn(var1);
   }

   private void updateValue(int var1, JDBCType var2, Object var3, JavaType var4, boolean var5) throws SQLServerException {
      this.updaterGetColumn(var1)
         .updateValue(var2, var3, var4, null, null, null, this.stmt.connection, this.stmt.stmtColumnEncriptionSetting, null, var5, var1);
   }

   private void updateValue(int var1, JDBCType var2, Object var3, JavaType var4, Calendar var5, boolean var6) throws SQLServerException {
      this.updaterGetColumn(var1)
         .updateValue(var2, var3, var4, null, var5, null, this.stmt.connection, this.stmt.stmtColumnEncriptionSetting, null, var6, var1);
   }

   private void updateValue(int var1, JDBCType var2, Object var3, JavaType var4, Integer var5, Integer var6, boolean var7) throws SQLServerException {
      this.updaterGetColumn(var1)
         .updateValue(var2, var3, var4, null, null, var6, this.stmt.connection, this.stmt.stmtColumnEncriptionSetting, var5, var7, var1);
   }

   private void updateStream(int var1, StreamType var2, Object var3, JavaType var4, long var5) throws SQLServerException {
      this.updaterGetColumn(var1)
         .updateValue(
            var2.getJDBCType(),
            var3,
            var4,
            new StreamSetterArgs(var2, var5),
            null,
            null,
            this.stmt.connection,
            this.stmt.stmtColumnEncriptionSetting,
            null,
            false,
            var1
         );
   }

   private final void updateSQLXMLInternal(int var1, SQLXML var2) throws SQLServerException {
      this.updaterGetColumn(var1)
         .updateValue(
            JDBCType.SQLXML,
            var2,
            JavaType.SQLXML,
            new StreamSetterArgs(StreamType.SQLXML, -1L),
            null,
            null,
            this.stmt.connection,
            this.stmt.stmtColumnEncriptionSetting,
            null,
            false,
            var1
         );
   }

   public void updateNull(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "updateNull", var1);
      this.checkClosed();
      this.updateValue(var1, this.updaterGetColumn(var1).getTypeInfo().getSSType().getJDBCType(), null, JavaType.OBJECT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNull");
   }

   public void updateBoolean(int var1, boolean var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBoolean", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BIT, var2, JavaType.BOOLEAN, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBoolean");
   }

   public void updateBoolean(int var1, boolean var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBoolean", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BIT, var2, JavaType.BOOLEAN, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBoolean");
   }

   public void updateByte(int var1, byte var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateByte", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TINYINT, var2, JavaType.BYTE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateByte");
   }

   public void updateByte(int var1, byte var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateByte", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TINYINT, var2, JavaType.BYTE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateByte");
   }

   public void updateShort(int var1, short var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateShort", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLINT, var2, JavaType.SHORT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateShort");
   }

   public void updateShort(int var1, short var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateShort", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLINT, var2, JavaType.SHORT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateShort");
   }

   public void updateInt(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateInt", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.INTEGER, var2, JavaType.INTEGER, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateInt");
   }

   public void updateInt(int var1, int var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateInt", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.INTEGER, var2, JavaType.INTEGER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateInt");
   }

   public void updateLong(int var1, long var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateLong", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BIGINT, var2, JavaType.LONG, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateLong");
   }

   public void updateLong(int var1, long var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateLong", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BIGINT, var2, JavaType.LONG, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateLong");
   }

   public void updateFloat(int var1, float var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateFloat", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.REAL, var2, JavaType.FLOAT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateFloat");
   }

   public void updateFloat(int var1, float var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateFloat", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.REAL, var2, JavaType.FLOAT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateFloat");
   }

   public void updateDouble(int var1, double var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDouble", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DOUBLE, var2, JavaType.DOUBLE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDouble");
   }

   public void updateDouble(int var1, double var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDouble", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DOUBLE, var2, JavaType.DOUBLE, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDouble");
   }

   public void updateMoney(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.MONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateMoney");
   }

   public void updateMoney(int var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.MONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateMoney");
   }

   public void updateMoney(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.MONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateMoney");
   }

   public void updateMoney(String var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.MONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateMoney");
   }

   public void updateSmallMoney(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallMoney");
   }

   public void updateSmallMoney(int var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallMoney");
   }

   public void updateSmallMoney(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallMoney");
   }

   public void updateSmallMoney(String var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallMoney");
   }

   public void updateBigDecimal(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateBigDecimal(int var1, BigDecimal var2, Integer var3, Integer var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateBigDecimal(int var1, BigDecimal var2, Integer var3, Integer var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2, var4, var5});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateString(int var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.VARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateString");
   }

   public void updateString(int var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.VARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateString");
   }

   public void updateNString(int var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNString", new Object[]{var1, var2});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.updateValue(var1, JDBCType.NVARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNString");
   }

   public void updateNString(int var1, String var2, boolean var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNString", new Object[]{var1, var2, var3});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.updateValue(var1, JDBCType.NVARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNString");
   }

   public void updateNString(String var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNString", new Object[]{var1, var2});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.NVARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNString");
   }

   public void updateNString(String var1, String var2, boolean var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNString", new Object[]{var1, var2, var3});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.NVARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNString");
   }

   public void updateBytes(int var1, byte[] var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBytes", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BINARY, var2, JavaType.BYTEARRAY, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBytes");
   }

   public void updateBytes(int var1, byte[] var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBytes", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BINARY, var2, JavaType.BYTEARRAY, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBytes");
   }

   public void updateDate(int var1, Date var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATE, var2, JavaType.DATE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDate");
   }

   public void updateDate(int var1, Date var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDate", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATE, var2, JavaType.DATE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDate");
   }

   public void updateTime(int var1, Time var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIME, var2, JavaType.TIME, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTime(int var1, Time var2, Integer var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIME, var2, JavaType.TIME, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTime(int var1, Time var2, Integer var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIME, var2, JavaType.TIME, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTimestamp(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateTimestamp(int var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateTimestamp(int var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateDateTime(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateDateTime(int var1, Timestamp var2, Integer var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIME, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateDateTime(int var1, Timestamp var2, Integer var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIME, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateSmallDateTime(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateSmallDateTime(int var1, Timestamp var2, Integer var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateSmallDateTime(int var1, Timestamp var2, Integer var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateDateTimeOffset(int var1, DateTimeOffset var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateDateTimeOffset(int var1, DateTimeOffset var2, Integer var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateDateTimeOffset(int var1, DateTimeOffset var2, Integer var3, boolean var4) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateUniqueIdentifier(int var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateUniqueIdentifier", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.GUID, var2, JavaType.STRING, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateUniqueIdentifier");
   }

   public void updateUniqueIdentifier(int var1, String var2, boolean var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateUniqueIdentifier", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.GUID, var2, JavaType.STRING, null, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateUniqueIdentifier");
   }

   public void updateAsciiStream(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateAsciiStream(int var1, InputStream var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateAsciiStream(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2, var3});
      this.checkClosed();
      this.updateStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateAsciiStream(String var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateAsciiStream(String var1, InputStream var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateAsciiStream(String var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateAsciiStream");
   }

   public void updateBinaryStream(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateBinaryStream(int var1, InputStream var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateBinaryStream(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateBinaryStream(String var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateBinaryStream(String var1, InputStream var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateBinaryStream(String var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBinaryStream");
   }

   public void updateCharacterStream(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateCharacterStream");
   }

   public void updateCharacterStream(int var1, Reader var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateCharacterStream");
   }

   public void updateCharacterStream(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateCharacterStream");
   }

   public void updateCharacterStream(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateCharacterStream");
   }

   public void updateCharacterStream(String var1, Reader var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateCharacterStream");
   }

   public void updateCharacterStream(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNCharacterStream");
   }

   public void updateNCharacterStream(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNCharacterStream");
   }

   public void updateNCharacterStream(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNCharacterStream");
   }

   public void updateNCharacterStream(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNCharacterStream");
   }

   public void updateNCharacterStream(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNCharacterStream");
   }

   public void updateObject(int var1, Object var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateObject(var1, var2, (Integer)null, null, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, SQLType var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateObject(var1, var2, (Integer)null, JDBCType.of(var3.getVendorTypeNumber()), null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateObject(var1, var2, var3, null, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateObject(var1, var2, var4, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var4, var5});
      }

      this.checkClosed();
      this.updateObject(var1, var2, var4, null, var3, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, SQLType var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateObject(var1, var2, var4, JDBCType.of(var3.getVendorTypeNumber()), null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(int var1, Object var2, SQLType var3, int var4, boolean var5) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.updateObject(var1, var2, var4, JDBCType.of(var3.getVendorTypeNumber()), null, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   private final void updateObject(int var1, Object var2, Integer var3, JDBCType var4, Integer var5, boolean var6) throws SQLServerException {
      Column var7 = this.updaterGetColumn(var1);
      SSType var8 = var7.getTypeInfo().getSSType();
      if (null == var2) {
         if (null == var4 || var4.isUnsupported()) {
            var4 = var8.getJDBCType();
         }

         var7.updateValue(var4, var2, JavaType.OBJECT, null, null, var3, this.stmt.connection, this.stmt.stmtColumnEncriptionSetting, var5, var6, var1);
      } else {
         JavaType var9 = JavaType.of(var2);
         JDBCType var10 = var9.getJDBCType(var8, var8.getJDBCType());
         if (null == var4) {
            var4 = var10;
         } else if (!var10.convertsTo(var4)) {
            DataTypes.throwConversionError(var10.toString(), var4.toString());
         }

         StreamSetterArgs var11 = null;
         switch (var9) {
            case READER:
               var11 = new StreamSetterArgs(StreamType.CHARACTER, -1L);
               break;
            case INPUTSTREAM:
               var11 = new StreamSetterArgs(var4.isTextual() ? StreamType.CHARACTER : StreamType.BINARY, -1L);
               break;
            case SQLXML:
               var11 = new StreamSetterArgs(StreamType.SQLXML, -1L);
         }

         var7.updateValue(var4, var2, var9, var11, null, var3, this.stmt.connection, this.stmt.stmtColumnEncriptionSetting, var5, var6, var1);
      }
   }

   public void updateNull(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "updateNull", var1);
      this.checkClosed();
      int var2 = this.findColumn(var1);
      this.updateValue(var2, this.updaterGetColumn(var2).getTypeInfo().getSSType().getJDBCType(), null, JavaType.OBJECT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNull");
   }

   public void updateBoolean(String var1, boolean var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBoolean", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BIT, var2, JavaType.BOOLEAN, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBoolean");
   }

   public void updateBoolean(String var1, boolean var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBoolean", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BIT, var2, JavaType.BOOLEAN, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBoolean");
   }

   public void updateByte(String var1, byte var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateByte", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateByte");
   }

   public void updateByte(String var1, byte var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateByte", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateByte");
   }

   public void updateShort(String var1, short var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateShort", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLINT, var2, JavaType.SHORT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateShort");
   }

   public void updateShort(String var1, short var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateShort", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLINT, var2, JavaType.SHORT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateShort");
   }

   public void updateInt(String var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateInt", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.INTEGER, var2, JavaType.INTEGER, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateInt");
   }

   public void updateInt(String var1, int var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateInt", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.INTEGER, var2, JavaType.INTEGER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateInt");
   }

   public void updateLong(String var1, long var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateLong", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BIGINT, var2, JavaType.LONG, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateLong");
   }

   public void updateLong(String var1, long var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateLong", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BIGINT, var2, JavaType.LONG, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateLong");
   }

   public void updateFloat(String var1, float var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateFloat", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.REAL, var2, JavaType.FLOAT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateFloat");
   }

   public void updateFloat(String var1, float var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateFloat", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.REAL, var2, JavaType.FLOAT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateFloat");
   }

   public void updateDouble(String var1, double var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDouble", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DOUBLE, var2, JavaType.DOUBLE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDouble");
   }

   public void updateDouble(String var1, double var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDouble", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DOUBLE, var2, JavaType.DOUBLE, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDouble");
   }

   public void updateBigDecimal(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateBigDecimal(String var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateBigDecimal(String var1, BigDecimal var2, Integer var3, Integer var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateBigDecimal(String var1, BigDecimal var2, Integer var3, Integer var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBigDecimal", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBigDecimal");
   }

   public void updateString(String var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.VARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateString");
   }

   public void updateString(String var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.VARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateString");
   }

   public void updateBytes(String var1, byte[] var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBytes", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTEARRAY, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBytes");
   }

   public void updateBytes(String var1, byte[] var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBytes", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTEARRAY, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBytes");
   }

   public void updateDate(String var1, Date var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATE, var2, JavaType.DATE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDate");
   }

   public void updateDate(String var1, Date var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDate", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATE, var2, JavaType.DATE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDate");
   }

   public void updateTime(String var1, Time var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTime(String var1, Time var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTime(String var1, Time var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTime");
   }

   public void updateTimestamp(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateTimestamp(String var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateTimestamp(String var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateTimestamp", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateTimestamp");
   }

   public void updateDateTime(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateDateTime(String var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIME, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateDateTime(String var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIME, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTime");
   }

   public void updateSmallDateTime(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateSmallDateTime(String var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateSmallDateTime(String var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSmallDateTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSmallDateTime");
   }

   public void updateDateTimeOffset(String var1, DateTimeOffset var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateDateTimeOffset(String var1, DateTimeOffset var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateDateTimeOffset(String var1, DateTimeOffset var2, int var3, boolean var4) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateDateTimeOffset", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "updateDateTimeOffset");
   }

   public void updateUniqueIdentifier(String var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateUniqueIdentifier", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.GUID, var2, JavaType.STRING, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateUniqueIdentifier");
   }

   public void updateUniqueIdentifier(String var1, String var2, boolean var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateUniqueIdentifier", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.GUID, var2, JavaType.STRING, null, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateUniqueIdentifier");
   }

   public void updateObject(String var1, Object var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, var3, null, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, var4, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, var4, null, var3, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2, SQLType var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, var4, JDBCType.of(var3.getVendorTypeNumber()), null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2, SQLType var3, int var4, boolean var5) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, var4, JDBCType.of(var3.getVendorTypeNumber()), null, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, (Integer)null, null, null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateObject(String var1, Object var2, SQLType var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateObject(this.findColumn(var1), var2, (Integer)null, JDBCType.of(var3.getVendorTypeNumber()), null, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateObject");
   }

   public void updateRowId(int var1, RowId var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public void updateRowId(String var1, RowId var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public void updateSQLXML(int var1, SQLXML var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSQLXML", new Object[]{var1, var2});
      }

      this.updateSQLXMLInternal(var1, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSQLXML");
   }

   public void updateSQLXML(String var1, SQLXML var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateSQLXML", new Object[]{var1, var2});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.updateSQLXMLInternal(this.findColumn(var1), var2);
      loggerExternal.exiting(this.getClassNameLogging(), "updateSQLXML");
   }

   public int getHoldability() throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getHoldability");
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      int var1 = 0 == this.stmt.getServerCursorId() ? 1 : this.stmt.getExecProps().getHoldability();
      loggerExternal.exiting(this.getClassNameLogging(), "getHoldability", var1);
      return var1;
   }

   public void insertRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "insertRow");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      if (!this.isOnInsertRow) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_mustBeOnInsertRow"), null, true);
      }

      Column var1 = null;

      for (int var2 = 0; var2 < this.columns.length; var2++) {
         if (this.columns[var2].hasUpdates()) {
            var1 = this.columns[var2];
            break;
         }

         if (null == var1 && this.columns[var2].isUpdatable()) {
            var1 = this.columns[var2];
         }
      }

      if (null == var1) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_noColumnParameterValue"), null, true);
      }

      assert var1.isUpdatable();
      assert null != var1.getTableName();

      final class InsertRowRPC extends TDSCommand {
         final String tableName;

         InsertRowRPC(String var2) {
            super("InsertRowRPC", 0);
            this.tableName = var2;
         }

         final boolean doExecute() throws SQLServerException {
            SQLServerResultSet.this.doInsertRowRPC(this, this.tableName);
            return true;
         }
      }

      this.stmt.executeCommand(new InsertRowRPC(var1.getTableName().asEscapedString()));
      if (-3 != this.rowCount) {
         this.rowCount++;
      }

      loggerExternal.exiting(this.getClassNameLogging(), "insertRow");
   }

   private final void doInsertRowRPC(TDSCommand var1, String var2) throws SQLServerException {
      assert 0 != this.serverCursorId;
      assert null != var2;
      assert var2.length() > 0;
      TDSWriter var3 = var1.startRequest((byte)3);
      var3.writeShort((short)-1);
      var3.writeShort((short)1);
      var3.writeByte((byte)0);
      var3.writeByte((byte)0);
      var3.writeRPCInt(null, new Integer(this.serverCursorId), false);
      var3.writeRPCInt(null, new Integer(4), false);
      var3.writeRPCInt(null, new Integer(this.fetchBufferGetRow()), false);
      if (this.hasUpdatedColumns()) {
         var3.writeRPCStringUnicode(var2);

         for (int var4 = 0; var4 < this.columns.length; var4++) {
            this.columns[var4].sendByRPC(var3, this.stmt.connection);
         }
      } else {
         var3.writeRPCStringUnicode("");
         var3.writeRPCStringUnicode("INSERT INTO " + var2 + " DEFAULT VALUES");
      }

      TDSParser.parse(var1.startResponse(), var1.getLogContext());
   }

   public void updateRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "updateRow");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      this.verifyResultSetIsNotOnInsertRow();
      this.verifyResultSetHasCurrentRow();
      this.verifyCurrentRowIsNotDeleted("R_cantUpdateDeletedRow");
      if (!this.hasUpdatedColumns()) {
         SQLServerException.makeFromDriverError(this.stmt.connection, this.stmt, SQLServerException.getErrString("R_noColumnParameterValue"), null, true);
      }

      try {
         final class UpdateRowRPC extends TDSCommand {
            UpdateRowRPC() {
               super("UpdateRowRPC", 0);
            }

            final boolean doExecute() throws SQLServerException {
               SQLServerResultSet.this.doUpdateRowRPC(this);
               return true;
            }
         }

         this.stmt.executeCommand(new UpdateRowRPC());
      } finally {
         this.cancelUpdates();
      }

      this.updatedCurrentRow = true;
      loggerExternal.exiting(this.getClassNameLogging(), "updateRow");
   }

   private final void doUpdateRowRPC(TDSCommand var1) throws SQLServerException {
      assert 0 != this.serverCursorId;
      TDSWriter var2 = var1.startRequest((byte)3);
      var2.writeShort((short)-1);
      var2.writeShort((short)1);
      var2.writeByte((byte)0);
      var2.writeByte((byte)0);
      var2.writeRPCInt(null, new Integer(this.serverCursorId), false);
      var2.writeRPCInt(null, new Integer(33), false);
      var2.writeRPCInt(null, new Integer(this.fetchBufferGetRow()), false);
      var2.writeRPCStringUnicode("");
      assert this.hasUpdatedColumns();

      for (int var3 = 0; var3 < this.columns.length; var3++) {
         this.columns[var3].sendByRPC(var2, this.stmt.connection);
      }

      TDSParser.parse(var1.startResponse(), var1.getLogContext());
   }

   final boolean hasUpdatedColumns() {
      for (int var1 = 0; var1 < this.columns.length; var1++) {
         if (this.columns[var1].hasUpdates()) {
            return true;
         }
      }

      return false;
   }

   public void deleteRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "deleteRow");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      this.verifyResultSetIsNotOnInsertRow();
      this.verifyResultSetHasCurrentRow();
      this.verifyCurrentRowIsNotDeleted("R_cantUpdateDeletedRow");

      try {
         final class DeleteRowRPC extends TDSCommand {
            DeleteRowRPC() {
               super("DeleteRowRPC", 0);
            }

            final boolean doExecute() throws SQLServerException {
               SQLServerResultSet.this.doDeleteRowRPC(this);
               return true;
            }
         }

         this.stmt.executeCommand(new DeleteRowRPC());
      } finally {
         this.cancelUpdates();
      }

      this.deletedCurrentRow = true;
      loggerExternal.exiting(this.getClassNameLogging(), "deleteRow");
   }

   private final void doDeleteRowRPC(TDSCommand var1) throws SQLServerException {
      assert 0 != this.serverCursorId;
      TDSWriter var2 = var1.startRequest((byte)3);
      var2.writeShort((short)-1);
      var2.writeShort((short)1);
      var2.writeByte((byte)0);
      var2.writeByte((byte)0);
      var2.writeRPCInt(null, new Integer(this.serverCursorId), false);
      var2.writeRPCInt(null, new Integer(34), false);
      var2.writeRPCInt(null, new Integer(this.fetchBufferGetRow()), false);
      var2.writeRPCStringUnicode("");
      TDSParser.parse(var1.startResponse(), var1.getLogContext());
   }

   public void refreshRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "refreshRow");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsScrollable();
      this.verifyResultSetIsUpdatable();
      this.verifyResultSetIsNotOnInsertRow();
      this.verifyResultSetHasCurrentRow();
      this.verifyCurrentRowIsNotDeleted("R_cantUpdateDeletedRow");
      if (1004 != this.stmt.getResultSetType() && 0 != this.serverCursorId) {
         this.cancelUpdates();
         this.doRefreshRow();
         loggerExternal.exiting(this.getClassNameLogging(), "refreshRow");
      }
   }

   private void doRefreshRow() throws SQLServerException {
      assert this.hasCurrentRow();
      int var1 = this.fetchBufferGetRow();
      this.doServerFetch(128, 0, 0);
      int var2 = 0;

      while (var2 < var1 && (this.isForwardOnly() ? this.fetchBufferNext() : this.scrollWindow.next(this))) {
         var2++;
      }

      if (var2 < var1) {
         this.currentRow = -1;
      } else {
         this.updatedCurrentRow = false;
      }
   }

   private final void cancelUpdates() {
      if (!this.isOnInsertRow) {
         this.clearColumnsValues();
      }
   }

   public void cancelRowUpdates() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "cancelRowUpdates");
      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      this.verifyResultSetIsNotOnInsertRow();
      this.cancelUpdates();
      loggerExternal.exiting(this.getClassNameLogging(), "cancelRowUpdates");
   }

   public void moveToInsertRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "moveToInsertRow");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      this.cancelUpdates();
      this.isOnInsertRow = true;
      loggerExternal.exiting(this.getClassNameLogging(), "moveToInsertRow");
   }

   public void moveToCurrentRow() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "moveToCurrentRow");
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + this.logCursorState());
      }

      this.checkClosed();
      this.verifyResultSetIsUpdatable();
      this.cancelInsert();
      loggerExternal.exiting(this.getClassNameLogging(), "moveToCurrentRow");
   }

   public Statement getStatement() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getStatement");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getStatement", this.stmt);
      return this.stmt;
   }

   public void updateClob(int var1, Clob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.CLOB, var2, JavaType.CLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateClob(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateClob(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateClob(String var1, Clob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.CLOB, var2, JavaType.CLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateClob(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateClob(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateClob");
   }

   public void updateNClob(int var1, NClob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.NCLOB, var2, JavaType.NCLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateNClob(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateNClob(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateNClob(String var1, NClob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.NCLOB, var2, JavaType.NCLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateNClob(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateNClob(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateNClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateNClob");
   }

   public void updateBlob(int var1, Blob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(var1, JDBCType.BLOB, var2, JavaType.BLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateBlob(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateBlob(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateBlob(String var1, Blob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateValue(this.findColumn(var1), JDBCType.BLOB, var2, JavaType.BLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateBlob(String var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateBlob(String var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "updateBlob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.updateStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "updateBlob");
   }

   public void updateArray(int var1, Array var2) throws SQLServerException {
      this.stmt.NotImplemented();
   }

   public void updateArray(String var1, Array var2) throws SQLServerException {
      this.stmt.NotImplemented();
   }

   public void updateRef(int var1, Ref var2) throws SQLServerException {
      this.stmt.NotImplemented();
   }

   public void updateRef(String var1, Ref var2) throws SQLServerException {
      this.stmt.NotImplemented();
   }

   public URL getURL(int var1) throws SQLServerException {
      this.stmt.NotImplemented();
      return null;
   }

   public URL getURL(String var1) throws SQLServerException {
      this.stmt.NotImplemented();
      return null;
   }

   final void doServerFetch(int var1, int var2, int var3) throws SQLServerException {
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " fetchType:" + var1 + " startRow:" + var2 + " numRows:" + var3);
      }

      this.discardFetchBuffer();
      this.fetchBuffer.init();
      SQLServerResultSet.CursorFetchCommand var4 = new SQLServerResultSet.CursorFetchCommand(this.serverCursorId, var1, var2, var3);
      this.stmt.executeCommand(var4);
      this.numFetchedRows = 0;
      this.resultSetCurrentRowType = RowType.UNKNOWN;
      this.areNullCompressedColumnsInitialized = false;
      this.lastColumnIndex = 0;
      if (null != this.scrollWindow && 128 != var1) {
         this.scrollWindow.resize(this.fetchSize);
      }

      if (var3 < 0 || var2 < 0) {
         try {
            while (this.scrollWindow.next(this)) {
            }
         } catch (SQLException var6) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " Ignored exception from row error during server cursor fixup: " + var6.getMessage());
            }
         }

         if (this.fetchBuffer.needsServerCursorFixup()) {
            this.doServerFetch(1, 0, 0);
            return;
         }

         this.scrollWindow.reset();
      }
   }

   private final void discardFetchBuffer() {
      this.fetchBuffer.clearStartMark();
      if (null != this.scrollWindow) {
         this.scrollWindow.clear();
      }

      try {
         while (this.fetchBufferNext()) {
         }
      } catch (SQLServerException var2) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this + " Encountered exception discarding fetch buffer: " + var2.getMessage());
         }
      }
   }

   final void closeServerCursor() {
      if (0 != this.serverCursorId) {
         if (this.stmt.connection.isSessionUnAvailable()) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this + ": Not closing cursor:" + this.serverCursorId + "; connection is already closed.");
            }
         } else {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " Closing cursor:" + this.serverCursorId);
            }

            try {
               final class CloseServerCursorCommand extends UninterruptableTDSCommand {
                  CloseServerCursorCommand() {
                     super("closeServerCursor");
                  }

                  final boolean doExecute() throws SQLServerException {
                     TDSWriter var1 = this.startRequest((byte)3);
                     var1.writeShort((short)-1);
                     var1.writeShort((short)9);
                     var1.writeByte((byte)0);
                     var1.writeByte((byte)0);
                     var1.writeRPCInt(null, new Integer(SQLServerResultSet.this.serverCursorId), false);
                     TDSParser.parse(this.startResponse(), this.getLogContext());
                     return true;
                  }
               }

               this.stmt.executeCommand(new CloseServerCursorCommand());
            } catch (SQLServerException var2) {
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + " Ignored error closing cursor:" + this.serverCursorId + " " + var2.getMessage());
               }
            }

            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " Closed cursor:" + this.serverCursorId);
            }
         }
      }
   }

   private final class CursorFetchCommand extends TDSCommand {
      private final int serverCursorId;
      private int fetchType;
      private int startRow;
      private int numRows;

      CursorFetchCommand(int var2, int var3, int var4, int var5) {
         super("doServerFetch", SQLServerResultSet.this.stmt.queryTimeout);
         this.serverCursorId = var2;
         this.fetchType = var3;
         this.startRow = var4;
         this.numRows = var5;
      }

      final boolean doExecute() throws SQLServerException {
         TDSWriter var1 = this.startRequest((byte)3);
         var1.writeShort((short)-1);
         var1.writeShort((short)7);
         var1.writeByte((byte)2);
         var1.writeByte((byte)0);
         var1.writeRPCInt(null, new Integer(this.serverCursorId), false);
         var1.writeRPCInt(null, new Integer(this.fetchType), false);
         var1.writeRPCInt(null, new Integer(this.startRow), false);
         var1.writeRPCInt(null, new Integer(this.numRows), false);
         SQLServerResultSet.this.tdsReader = this.startResponse(
            SQLServerResultSet.this.isForwardOnly()
               && 1007 != SQLServerResultSet.this.stmt.resultSetConcurrency
               && SQLServerResultSet.this.stmt.getExecProps().wasResponseBufferingSet()
               && SQLServerResultSet.this.stmt.getExecProps().isResponseBufferingAdaptive()
         );
         return false;
      }

      final void processResponse(TDSReader var1) throws SQLServerException {
         SQLServerResultSet.this.tdsReader = var1;
         SQLServerResultSet.this.discardFetchBuffer();
      }
   }

   private final class FetchBuffer {
      private final SQLServerResultSet.FetchBuffer.FetchBufferTokenHandler fetchBufferTokenHandler = new SQLServerResultSet.FetchBuffer.FetchBufferTokenHandler(
         
      );
      private TDSReaderMark startMark;
      private RowType fetchBufferCurrentRowType = RowType.UNKNOWN;
      private boolean done;
      private boolean needsServerCursorFixup;

      final void clearStartMark() {
         this.startMark = null;
      }

      final boolean needsServerCursorFixup() {
         return this.needsServerCursorFixup;
      }

      FetchBuffer() {
         this.init();
      }

      final void ensureStartMark() {
         if (null == this.startMark && !SQLServerResultSet.this.isForwardOnly()) {
            if (SQLServerResultSet.logger.isLoggable(Level.FINEST)) {
               SQLServerResultSet.logger.finest(this.toString() + " Setting fetch buffer start mark");
            }

            this.startMark = SQLServerResultSet.this.tdsReader.mark();
         }
      }

      final void reset() {
         assert null != SQLServerResultSet.this.tdsReader;
         assert null != this.startMark;
         SQLServerResultSet.this.tdsReader.reset(this.startMark);
         this.fetchBufferCurrentRowType = RowType.UNKNOWN;
         this.done = false;
      }

      final void init() {
         this.startMark = 0 == SQLServerResultSet.this.serverCursorId && !SQLServerResultSet.this.isForwardOnly()
            ? SQLServerResultSet.this.tdsReader.mark()
            : null;
         this.fetchBufferCurrentRowType = RowType.UNKNOWN;
         this.done = false;
         this.needsServerCursorFixup = false;
      }

      final RowType nextRow() throws SQLServerException {
         this.fetchBufferCurrentRowType = RowType.UNKNOWN;

         while (null != SQLServerResultSet.this.tdsReader && !this.done && this.fetchBufferCurrentRowType.equals(RowType.UNKNOWN)) {
            TDSParser.parse(SQLServerResultSet.this.tdsReader, this.fetchBufferTokenHandler);
         }

         if (this.fetchBufferCurrentRowType.equals(RowType.UNKNOWN) && null != this.fetchBufferTokenHandler.getDatabaseError()) {
            SQLServerException.makeFromDatabaseError(
               SQLServerResultSet.this.stmt.connection,
               null,
               this.fetchBufferTokenHandler.getDatabaseError().getMessage(),
               this.fetchBufferTokenHandler.getDatabaseError(),
               false
            );
         }

         return this.fetchBufferCurrentRowType;
      }

      private final class FetchBufferTokenHandler extends TDSTokenHandler {
         FetchBufferTokenHandler() {
            super("FetchBufferTokenHandler");
         }

         boolean onColMetaData(TDSReader var1) throws SQLServerException {
            new StreamColumns(Util.shouldHonorAEForRead(SQLServerResultSet.this.stmt.stmtColumnEncriptionSetting, SQLServerResultSet.this.stmt.connection))
               .setFromTDS(var1);
            return true;
         }

         boolean onRow(TDSReader var1) throws SQLServerException {
            FetchBuffer.this.ensureStartMark();
            if (209 != var1.readUnsignedByte()) {
               assert false;
            }

            FetchBuffer.this.fetchBufferCurrentRowType = RowType.ROW;
            return false;
         }

         boolean onNBCRow(TDSReader var1) throws SQLServerException {
            FetchBuffer.this.ensureStartMark();
            if (210 != var1.readUnsignedByte()) {
               assert false;
            }

            FetchBuffer.this.fetchBufferCurrentRowType = RowType.NBCROW;
            return false;
         }

         boolean onDone(TDSReader var1) throws SQLServerException {
            FetchBuffer.this.ensureStartMark();
            StreamDone var2 = new StreamDone();
            var2.setFromTDS(var1);
            FetchBuffer.this.done = true;
            return 0 != SQLServerResultSet.this.serverCursorId;
         }

         boolean onRetStatus(TDSReader var1) throws SQLServerException {
            StreamRetStatus var2 = new StreamRetStatus();
            var2.setFromTDS(var1);
            FetchBuffer.this.needsServerCursorFixup = 2 == var2.getStatus();
            return true;
         }

         void onEOF(TDSReader var1) throws SQLServerException {
            super.onEOF(var1);
            FetchBuffer.this.done = true;
         }
      }
   }
}
