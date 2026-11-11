package com.microsoft.sqlserver.jdbc;

import com.microsoft.sqlserver.jdbc.SQLServerStatement.StmtExecOutParamHandler;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import microsoft.sql.DateTimeOffset;

public class SQLServerPreparedStatement extends SQLServerStatement implements ISQLServerPreparedStatement {
   boolean isInternalEncryptionQuery = false;
   private static final int BATCH_STATEMENT_DELIMITER_TDS_71 = 128;
   private static final int BATCH_STATEMENT_DELIMITER_TDS_72 = 255;
   final int nBatchStatementDelimiter = 255;
   private String sqlCommand;
   private String preparedTypeDefinitions;
   final String userSQL;
   private String preparedSQL;
   private ArrayList<String> parameterNames;
   final boolean bReturnValueSyntax;
   int outParamIndexAdjustment;
   ArrayList<Parameter[]> batchParamValues;
   private int prepStmtHandle = 0;
   private boolean expectPrepStmtHandle = false;

   String getClassNameInternal() {
      return "SQLServerPreparedStatement";
   }

   SQLServerPreparedStatement(SQLServerConnection var1, String var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      super(var1, var3, var4, var5);
      this.stmtPoolable = true;
      this.sqlCommand = var2;
      JDBCSyntaxTranslator var6 = new JDBCSyntaxTranslator();
      var2 = var6.translate(var2);
      this.procedureName = var6.getProcedureName();
      this.bReturnValueSyntax = var6.hasReturnValueSyntax();
      this.userSQL = var2;
      this.initParams(this.userSQL);
   }

   private void closePreparedHandle() {
      if (0 != this.prepStmtHandle) {
         if (this.connection.isSessionUnAvailable()) {
            if (this.getStatementLogger().isLoggable(Level.FINER)) {
               this.getStatementLogger().finer(this + ": Not closing PreparedHandle:" + this.prepStmtHandle + "; connection is already closed.");
            }
         } else {
            if (this.getStatementLogger().isLoggable(Level.FINER)) {
               this.getStatementLogger().finer(this + ": Closing PreparedHandle:" + this.prepStmtHandle);
            }

            try {
               final class PreparedHandleClose extends UninterruptableTDSCommand {
                  PreparedHandleClose() {
                     super("closePreparedHandle");
                  }

                  final boolean doExecute() throws SQLServerException {
                     TDSWriter var1 = this.startRequest((byte)3);
                     var1.writeShort((short)-1);
                     var1.writeShort((short)(SQLServerPreparedStatement.this.executedSqlDirectly ? 15 : 6));
                     var1.writeByte((byte)0);
                     var1.writeByte((byte)0);
                     var1.writeRPCInt(null, new Integer(SQLServerPreparedStatement.this.prepStmtHandle), false);
                     SQLServerPreparedStatement.this.prepStmtHandle = 0;
                     TDSParser.parse(this.startResponse(), this.getLogContext());
                     return true;
                  }
               }

               this.executeCommand(new PreparedHandleClose());
            } catch (SQLServerException var2) {
               if (this.getStatementLogger().isLoggable(Level.FINER)) {
                  this.getStatementLogger().log(Level.FINER, this + ": Error (ignored) closing PreparedHandle:" + this.prepStmtHandle, var2);
               }
            }

            if (this.getStatementLogger().isLoggable(Level.FINER)) {
               this.getStatementLogger().finer(this + ": Closed PreparedHandle:" + this.prepStmtHandle);
            }
         }
      }
   }

   final void closeInternal() {
      super.closeInternal();
      this.closePreparedHandle();
      this.batchParamValues = null;
   }

   final void initParams(String var1) {
      int var2 = 0;
      int var3 = -1;

      while (true) {
         var3++;
         if ((var3 = ParameterUtils.scanSQLForChar('?', var1, var3)) >= var1.length()) {
            this.inOutParam = new Parameter[var2];

            for (int var4 = 0; var4 < var2; var4++) {
               this.inOutParam[var4] = new Parameter(Util.shouldHonorAEForParameters(this.stmtColumnEncriptionSetting, this.connection));
            }

            return;
         }

         var2++;
      }
   }

   public final void clearParameters() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "clearParameters");
      this.checkClosed();
      if (this.inOutParam != null) {
         for (int var1 = 0; var1 < this.inOutParam.length; var1++) {
            this.inOutParam[var1].clearInputValue();
         }

         loggerExternal.exiting(this.getClassNameLogging(), "clearParameters");
      }
   }

   private final boolean buildPreparedStrings(Parameter[] var1, boolean var2) throws SQLServerException {
      String var3 = this.buildParamTypeDefinitions(var1, var2);
      if (null != this.preparedTypeDefinitions && var3.equals(this.preparedTypeDefinitions)) {
         return false;
      }

      this.preparedTypeDefinitions = var3;
      this.preparedSQL = this.connection.replaceParameterMarkers(this.userSQL, var1, this.bReturnValueSyntax);
      if (this.bRequestedGeneratedKeys) {
         this.preparedSQL = this.preparedSQL + " select SCOPE_IDENTITY() AS GENERATED_KEYS";
      }

      return true;
   }

   private String buildParamTypeDefinitions(Parameter[] var1, boolean var2) throws SQLServerException {
      StringBuilder var3 = new StringBuilder();
      int var4 = var1.length;
      char[] var5 = new char[10];
      this.parameterNames = new ArrayList();

      for (int var6 = 0; var6 < var4; var6++) {
         if (var6 > 0) {
            var3.append(',');
         }

         int var7 = SQLServerConnection.makeParamName(var6, var5, 0);

         for (int var8 = 0; var8 < var7; var8++) {
            var3.append(var5[var8]);
         }

         var3.append(' ');
         this.parameterNames.add(var6, new String(var5).trim());
         var1[var6].renewDefinition = var2;
         String var11 = var1[var6].getTypeDefinition(this.connection, this.resultsReader());
         if (null == var11) {
            MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_valueNotSetForParameter"));
            Object[] var10 = new Object[]{new Integer(var6 + 1)};
            SQLServerException.makeFromDriverError(this.connection, this, var9.format(var10), null, false);
         }

         var3.append(var11);
         if (var1[var6].isOutput()) {
            var3.append(" OUTPUT");
         }
      }

      return var3.toString();
   }

   public ResultSet executeQuery() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "executeQuery");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.executeStatement(new SQLServerPreparedStatement.PrepStmtExecCmd(this, 1));
      loggerExternal.exiting(this.getClassNameLogging(), "executeQuery");
      return this.resultSet;
   }

   final ResultSet executeQueryInternal() throws SQLServerException {
      this.checkClosed();
      this.executeStatement(new SQLServerPreparedStatement.PrepStmtExecCmd(this, 5));
      return this.resultSet;
   }

   public int executeUpdate() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "executeUpdate");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.executeStatement(new SQLServerPreparedStatement.PrepStmtExecCmd(this, 2));
      if (this.updateCount < -2147483648L || this.updateCount > 2147483647L) {
         SQLServerException.makeFromDriverError(this.connection, this, SQLServerException.getErrString("R_updateCountOutofRange"), null, true);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "executeUpdate", new Long(this.updateCount));
      return (int)this.updateCount;
   }

   public long executeLargeUpdate() throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      loggerExternal.entering(this.getClassNameLogging(), "executeLargeUpdate");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.executeStatement(new SQLServerPreparedStatement.PrepStmtExecCmd(this, 2));
      loggerExternal.exiting(this.getClassNameLogging(), "executeLargeUpdate", new Long(this.updateCount));
      return this.updateCount;
   }

   public boolean execute() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "execute");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.executeStatement(new SQLServerPreparedStatement.PrepStmtExecCmd(this, 3));
      loggerExternal.exiting(this.getClassNameLogging(), "execute", null != this.resultSet);
      return null != this.resultSet;
   }

   final void doExecutePreparedStatement(SQLServerPreparedStatement.PrepStmtExecCmd var1) throws SQLServerException {
      this.resetForReexecute();
      this.setMaxRowsAndMaxFieldSize();
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      boolean var2 = this.buildPreparedStrings(this.inOutParam, false);
      if (Util.shouldHonorAEForParameters(this.stmtColumnEncriptionSetting, this.connection) && 0 < this.inOutParam.length && !this.isInternalEncryptionQuery) {
         this.getParameterEncryptionMetadata(this.inOutParam);
         this.setMaxRowsAndMaxFieldSize();
         this.buildPreparedStrings(this.inOutParam, true);
      }

      TDSWriter var3 = var1.startRequest((byte)3);
      this.doPrepExec(var3, this.inOutParam, var2);
      this.ensureExecuteResultsReader(var1.startResponse(this.getIsResponseBufferingAdaptive()));
      this.startResults();
      this.getNextResult();
      if (1 == this.executeMethod && null == this.resultSet) {
         SQLServerException.makeFromDriverError(this.connection, this, SQLServerException.getErrString("R_noResultset"), null, true);
      } else if (2 == this.executeMethod && null != this.resultSet) {
         SQLServerException.makeFromDriverError(this.connection, this, SQLServerException.getErrString("R_resultsetGeneratedForUpdate"), null, false);
      }
   }

   boolean consumeExecOutParam(TDSReader var1) throws SQLServerException {
      if (!this.expectPrepStmtHandle && !this.expectCursorOutParams) {
         return false;
      }

      final class PrepStmtExecOutParamHandler extends StmtExecOutParamHandler {
         PrepStmtExecOutParamHandler() {
            super(SQLServerPreparedStatement.this);
         }

         boolean onRetValue(TDSReader var1) throws SQLServerException {
            if (!SQLServerPreparedStatement.this.expectPrepStmtHandle) {
               return super.onRetValue(var1);
            }

            SQLServerPreparedStatement.this.expectPrepStmtHandle = false;
            Parameter var2 = new Parameter(
               Util.shouldHonorAEForParameters(SQLServerPreparedStatement.this.stmtColumnEncriptionSetting, SQLServerPreparedStatement.this.connection)
            );
            var2.skipRetValStatus(var1);
            SQLServerPreparedStatement.this.prepStmtHandle = var2.getInt(var1);
            var2.skipValue(var1, true);
            if (SQLServerPreparedStatement.this.getStatementLogger().isLoggable(Level.FINER)) {
               SQLServerPreparedStatement.this.getStatementLogger()
                  .finer(this.toString() + ": Setting PreparedHandle:" + SQLServerPreparedStatement.this.prepStmtHandle);
            }

            return true;
         }
      }

      TDSParser.parse(var1, new PrepStmtExecOutParamHandler());
      return true;
   }

   void sendParamsByRPC(TDSWriter var1, Parameter[] var2) throws SQLServerException {
      for (int var4 = 0; var4 < var2.length; var4++) {
         if (JDBCType.TVP == var2[var4].getJdbcType()) {
            char[] var3 = new char[10];
            int var5 = SQLServerConnection.makeParamName(var4, var3, 0);
            var1.writeByte((byte)var5);
            var1.writeString(new String(var3, 0, var5));
         }

         var2[var4].sendByRPC(var1, this.connection);
      }
   }

   private final void buildServerCursorPrepExecParams(TDSWriter var1) throws SQLServerException {
      if (this.getStatementLogger().isLoggable(Level.FINE)) {
         this.getStatementLogger().fine(this.toString() + ": calling sp_cursorprepexec: PreparedHandle:" + this.prepStmtHandle + ", SQL:" + this.preparedSQL);
      }

      this.expectPrepStmtHandle = true;
      this.executedSqlDirectly = false;
      this.expectCursorOutParams = true;
      this.outParamIndexAdjustment = 7;
      var1.writeShort((short)-1);
      var1.writeShort((short)5);
      var1.writeByte((byte)0);
      var1.writeByte((byte)0);
      var1.writeRPCInt(null, new Integer(this.prepStmtHandle), true);
      this.prepStmtHandle = 0;
      var1.writeRPCInt(null, new Integer(0), true);
      var1.writeRPCStringUnicode(this.preparedTypeDefinitions.length() > 0 ? this.preparedTypeDefinitions : null);
      var1.writeRPCStringUnicode(this.preparedSQL);
      var1.writeRPCInt(null, new Integer(this.getResultSetScrollOpt() & ~(0 == this.preparedTypeDefinitions.length() ? 4096 : 0)), false);
      var1.writeRPCInt(null, new Integer(this.getResultSetCCOpt()), false);
      var1.writeRPCInt(null, new Integer(0), true);
   }

   private final void buildPrepExecParams(TDSWriter var1) throws SQLServerException {
      if (this.getStatementLogger().isLoggable(Level.FINE)) {
         this.getStatementLogger().fine(this.toString() + ": calling sp_prepexec: PreparedHandle:" + this.prepStmtHandle + ", SQL:" + this.preparedSQL);
      }

      this.expectPrepStmtHandle = true;
      this.executedSqlDirectly = true;
      this.expectCursorOutParams = false;
      this.outParamIndexAdjustment = 3;
      var1.writeShort((short)-1);
      var1.writeShort((short)13);
      var1.writeByte((byte)0);
      var1.writeByte((byte)0);
      var1.writeRPCInt(null, new Integer(this.prepStmtHandle), true);
      this.prepStmtHandle = 0;
      var1.writeRPCStringUnicode(this.preparedTypeDefinitions.length() > 0 ? this.preparedTypeDefinitions : null);
      var1.writeRPCStringUnicode(this.preparedSQL);
   }

   private final void buildServerCursorExecParams(TDSWriter var1) throws SQLServerException {
      if (this.getStatementLogger().isLoggable(Level.FINE)) {
         this.getStatementLogger().fine(this.toString() + ": calling sp_cursorexecute: PreparedHandle:" + this.prepStmtHandle + ", SQL:" + this.preparedSQL);
      }

      this.expectPrepStmtHandle = false;
      this.executedSqlDirectly = false;
      this.expectCursorOutParams = true;
      this.outParamIndexAdjustment = 5;
      var1.writeShort((short)-1);
      var1.writeShort((short)4);
      var1.writeByte((byte)0);
      var1.writeByte((byte)0);
      assert 0 != this.prepStmtHandle;
      var1.writeRPCInt(null, new Integer(this.prepStmtHandle), false);
      var1.writeRPCInt(null, new Integer(0), true);
      var1.writeRPCInt(null, new Integer(this.getResultSetScrollOpt() & -4097), false);
      var1.writeRPCInt(null, new Integer(this.getResultSetCCOpt()), false);
      var1.writeRPCInt(null, new Integer(0), true);
   }

   private final void buildExecParams(TDSWriter var1) throws SQLServerException {
      if (this.getStatementLogger().isLoggable(Level.FINE)) {
         this.getStatementLogger().fine(this.toString() + ": calling sp_execute: PreparedHandle:" + this.prepStmtHandle + ", SQL:" + this.preparedSQL);
      }

      this.expectPrepStmtHandle = false;
      this.executedSqlDirectly = true;
      this.expectCursorOutParams = false;
      this.outParamIndexAdjustment = 1;
      var1.writeShort((short)-1);
      var1.writeShort((short)12);
      var1.writeByte((byte)0);
      var1.writeByte((byte)0);
      assert 0 != this.prepStmtHandle;
      var1.writeRPCInt(null, new Integer(this.prepStmtHandle), false);
   }

   private void getParameterEncryptionMetadata(Parameter[] var1) throws SQLServerException {
      SQLServerResultSet var2 = null;
      SQLServerCallableStatement var3 = null;
      assert this.connection != null : "Connection should not be null";

      try {
         if (this.getStatementLogger().isLoggable(Level.FINE)) {
            this.getStatementLogger().fine("Calling stored procedure sp_describe_parameter_encryption to get parameter encryption information.");
         }

         var3 = (SQLServerCallableStatement)this.connection.prepareCall("exec sp_describe_parameter_encryption ?,?");
         var3.isInternalEncryptionQuery = true;
         var3.setNString(1, this.preparedSQL);
         var3.setNString(2, this.preparedTypeDefinitions);
         var2 = (SQLServerResultSet)var3.executeQueryInternal();
      } catch (SQLException var15) {
         if (var15 instanceof SQLServerException) {
            throw (SQLServerException)var15;
         }

         throw new SQLServerException(SQLServerException.getErrString("R_UnableRetrieveParameterMetadata"), null, 0, var15);
      }

      if (null != var2) {
         HashMap var4 = new HashMap();
         CekTableEntry var5 = null;

         try {
            while (var2.next()) {
               int var6 = var2.getInt(DescribeParameterEncryptionResultSet1.KeyOrdinal.value());
               if (!var4.containsKey(var6)) {
                  var5 = new CekTableEntry(var6);
                  var4.put(var5.ordinal, var5);
               } else {
                  var5 = (CekTableEntry)var4.get(var6);
               }

               var5.add(
                  var2.getBytes(DescribeParameterEncryptionResultSet1.EncryptedKey.value()),
                  var2.getInt(DescribeParameterEncryptionResultSet1.DbId.value()),
                  var2.getInt(DescribeParameterEncryptionResultSet1.KeyId.value()),
                  var2.getInt(DescribeParameterEncryptionResultSet1.KeyVersion.value()),
                  var2.getBytes(DescribeParameterEncryptionResultSet1.KeyMdVersion.value()),
                  var2.getString(DescribeParameterEncryptionResultSet1.KeyPath.value()),
                  var2.getString(DescribeParameterEncryptionResultSet1.ProviderName.value()),
                  var2.getString(DescribeParameterEncryptionResultSet1.KeyEncryptionAlgorithm.value())
               );
            }

            if (this.getStatementLogger().isLoggable(Level.FINE)) {
               this.getStatementLogger().fine("Matadata of CEKs is retrieved.");
            }
         } catch (SQLException var14) {
            if (var14 instanceof SQLServerException) {
               throw (SQLServerException)var14;
            }

            throw new SQLServerException(SQLServerException.getErrString("R_UnableRetrieveParameterMetadata"), null, 0, var14);
         }

         if (!var3.getMoreResults()) {
            throw new SQLServerException(this, SQLServerException.getErrString("R_UnexpectedDescribeParamFormat"), null, 0, false);
         }

         int var21 = 0;

         try {
            var2 = (SQLServerResultSet)var3.getResultSet();

            while (var2.next()) {
               var21++;
               String var7 = var2.getString(DescribeParameterEncryptionResultSet2.ParameterName.value());
               int var8 = this.parameterNames.indexOf(var7);
               int var9 = var2.getInt(DescribeParameterEncryptionResultSet2.ColumnEncryptionKeyOrdinal.value());
               var5 = (CekTableEntry)var4.get(var9);
               if (null != var5 && var4.size() < var9) {
                  MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_InvalidEncryptionKeyOridnal"));
                  Object[] var25 = new Object[]{var9, var5.getSize()};
                  throw new SQLServerException(this, var24.format(var25), null, 0, false);
               }

               SQLServerEncryptionType var10 = SQLServerEncryptionType.of((byte)var2.getInt(DescribeParameterEncryptionResultSet2.ColumnEncrytionType.value()));
               if (SQLServerEncryptionType.PlainText != var10) {
                  var1[var8].cryptoMeta = new CryptoMetadata(
                     var5,
                     (short)var9,
                     (byte)var2.getInt(DescribeParameterEncryptionResultSet2.ColumnEncryptionAlgorithm.value()),
                     null,
                     var10.value,
                     (byte)var2.getInt(DescribeParameterEncryptionResultSet2.NormalizationRuleVersion.value())
                  );
                  SQLServerSecurityUtility.decryptSymmetricKey(var1[var8].cryptoMeta, this.connection);
               } else if (var1[var8].getForceEncryption()) {
                  MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_ForceEncryptionTrue_HonorAETrue_UnencryptedColumn"));
                  Object[] var12 = new Object[]{this.userSQL, var8 + 1};
                  SQLServerException.makeFromDriverError(this.connection, this, var11.format(var12), null, true);
               }
            }

            if (this.getStatementLogger().isLoggable(Level.FINE)) {
               this.getStatementLogger().fine("Parameter encryption metadata is set.");
            }
         } catch (SQLException var13) {
            if (var13 instanceof SQLServerException) {
               throw (SQLServerException)var13;
            }

            throw new SQLServerException(SQLServerException.getErrString("R_UnableRetrieveParameterMetadata"), null, 0, var13);
         }

         if (var21 != var1.length) {
            MessageFormat var22 = new MessageFormat(SQLServerException.getErrString("R_MissingParamEncryptionMetadata"));
            Object[] var23 = new Object[]{this.userSQL};
            throw new SQLServerException(this, var22.format(var23), null, 0, false);
         }

         var2.close();
         if (null != var3) {
            var3.close();
         }

         this.connection.resetCurrentCommand();
      }
   }

   private final boolean doPrepExec(TDSWriter var1, Parameter[] var2, boolean var3) throws SQLServerException {
      boolean var4 = var3 || 0 == this.prepStmtHandle;
      if (var4) {
         if (this.isCursorable(this.executeMethod)) {
            this.buildServerCursorPrepExecParams(var1);
         } else {
            this.buildPrepExecParams(var1);
         }
      } else if (this.isCursorable(this.executeMethod)) {
         this.buildServerCursorExecParams(var1);
      } else {
         this.buildExecParams(var1);
      }

      this.sendParamsByRPC(var1, var2);
      return var4;
   }

   public final ResultSetMetaData getMetaData() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMetaData");
      this.checkClosed();
      boolean var1 = false;
      ResultSetMetaData var2 = null;

      try {
         if (this.resultSet != null) {
            this.resultSet.checkClosed();
         }
      } catch (SQLServerException var4) {
         var1 = true;
      }

      if (this.resultSet == null || var1) {
         SQLServerResultSet var3 = (SQLServerResultSet)this.buildExecuteMetaData();
         if (null != var3) {
            var2 = var3.getMetaData();
         }
      } else if (this.resultSet != null) {
         var2 = this.resultSet.getMetaData();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getMetaData", var2);
      return var2;
   }

   private ResultSet buildExecuteMetaData() throws SQLServerException {
      String var1 = this.sqlCommand;
      if (var1.indexOf(123) >= 0) {
         var1 = new JDBCSyntaxTranslator().translate(var1);
      }

      SQLServerResultSet var2 = null;

      try {
         var1 = replaceMarkerWithNull(var1);
         SQLServerStatement var3 = (SQLServerStatement)this.connection.createStatement();
         var2 = var3.executeQueryInternal("set fmtonly on " + var1 + "\nset fmtonly off");
      } catch (SQLException var6) {
         if (!var6.getMessage().equals(SQLServerException.getErrString("R_noResultset"))) {
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_processingError"));
            Object[] var5 = new Object[]{new String(var6.getMessage())};
            SQLServerException.makeFromDriverError(this.connection, this, var4.format(var5), null, true);
         }
      }

      return var2;
   }

   final Parameter setterGetParam(int var1) throws SQLServerException {
      if (var1 < 1 || var1 > this.inOutParam.length) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_indexOutOfRange"));
         Object[] var3 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.connection, this, var2.format(var3), "07009", false);
      }

      return this.inOutParam[var1 - 1];
   }

   final void setValue(int var1, JDBCType var2, Object var3, JavaType var4, String var5) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(var2, var3, var4, null, null, null, null, this.connection, false, this.stmtColumnEncriptionSetting, var1, this.userSQL, var5);
   }

   final void setValue(int var1, JDBCType var2, Object var3, JavaType var4, boolean var5) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(var2, var3, var4, null, null, null, null, this.connection, var5, this.stmtColumnEncriptionSetting, var1, this.userSQL, null);
   }

   final void setValue(int var1, JDBCType var2, Object var3, JavaType var4, Integer var5, Integer var6, boolean var7) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(var2, var3, var4, null, null, var5, var6, this.connection, var7, this.stmtColumnEncriptionSetting, var1, this.userSQL, null);
   }

   final void setValue(int var1, JDBCType var2, Object var3, JavaType var4, Calendar var5, boolean var6) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(var2, var3, var4, null, var5, null, null, this.connection, var6, this.stmtColumnEncriptionSetting, var1, this.userSQL, null);
   }

   final void setStream(int var1, StreamType var2, Object var3, JavaType var4, long var5) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(
            var2.getJDBCType(),
            var3,
            var4,
            new StreamSetterArgs(var2, var5),
            null,
            null,
            null,
            this.connection,
            false,
            this.stmtColumnEncriptionSetting,
            var1,
            this.userSQL,
            null
         );
   }

   final void setSQLXMLInternal(int var1, SQLXML var2) throws SQLServerException {
      this.setterGetParam(var1)
         .setValue(
            JDBCType.SQLXML,
            var2,
            JavaType.SQLXML,
            new StreamSetterArgs(StreamType.SQLXML, -1L),
            null,
            null,
            null,
            this.connection,
            false,
            this.stmtColumnEncriptionSetting,
            var1,
            this.userSQL,
            null
         );
   }

   public final void setAsciiStream(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   public final void setAsciiStream(int var1, InputStream var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   public final void setAsciiStream(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   private final Parameter getParam(int var1) throws SQLServerException {
      var1--;
      if (var1 < 0 || var1 >= this.inOutParam.length) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_indexOutOfRange"));
         Object[] var3 = new Object[]{new Integer(var1 + 1)};
         SQLServerException.makeFromDriverError(this.connection, this, var2.format(var3), "07009", false);
      }

      return this.inOutParam[var1];
   }

   public final void setBigDecimal(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public final void setBigDecimal(int var1, BigDecimal var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public final void setBigDecimal(int var1, BigDecimal var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public final void setMoney(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.MONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setMoney");
   }

   public final void setMoney(int var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.MONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setMoney");
   }

   public final void setSmallMoney(int var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallMoney");
   }

   public final void setSmallMoney(int var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallMoney");
   }

   public final void setBinaryStream(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStreaml", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBinaryStream(int var1, InputStream var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBinaryStream(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBoolean(int var1, boolean var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBoolean", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BIT, var2, JavaType.BOOLEAN, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBoolean");
   }

   public final void setBoolean(int var1, boolean var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBoolean", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BIT, var2, JavaType.BOOLEAN, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBoolean");
   }

   public final void setByte(int var1, byte var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setByte", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TINYINT, var2, JavaType.BYTE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setByte");
   }

   public final void setByte(int var1, byte var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setByte", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TINYINT, var2, JavaType.BYTE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setByte");
   }

   public final void setBytes(int var1, byte[] var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBytes", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BINARY, var2, JavaType.BYTEARRAY, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBytes");
   }

   public final void setBytes(int var1, byte[] var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBytes", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BINARY, var2, JavaType.BYTEARRAY, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBytes");
   }

   public final void setUniqueIdentifier(int var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setUniqueIdentifier", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.GUID, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setUniqueIdentifier");
   }

   public final void setUniqueIdentifier(int var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setUniqueIdentifier", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.GUID, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setUniqueIdentifier");
   }

   public final void setDouble(int var1, double var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDouble", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DOUBLE, var2, JavaType.DOUBLE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDouble");
   }

   public final void setDouble(int var1, double var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDouble", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DOUBLE, var2, JavaType.DOUBLE, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDouble");
   }

   public final void setFloat(int var1, float var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setFloat", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.REAL, var2, JavaType.FLOAT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setFloat");
   }

   public final void setFloat(int var1, float var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setFloat", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.REAL, var2, JavaType.FLOAT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setFloat");
   }

   public final void setInt(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setInt", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.INTEGER, var2, JavaType.INTEGER, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setInt");
   }

   public final void setInt(int var1, int var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setInt", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.INTEGER, var2, JavaType.INTEGER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setInt");
   }

   public final void setLong(int var1, long var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setLong", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BIGINT, var2, JavaType.LONG, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setLong");
   }

   public final void setLong(int var1, long var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setLong", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BIGINT, var2, JavaType.LONG, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setLong");
   }

   public final void setNull(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNull", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setObject(this.setterGetParam(var1), null, JavaType.OBJECT, JDBCType.of(var2), null, null, false, var1, null);
      loggerExternal.exiting(this.getClassNameLogging(), "setNull");
   }

   final void setObjectNoType(int var1, Object var2, boolean var3) throws SQLServerException {
      Parameter var4 = this.setterGetParam(var1);
      JDBCType var5 = var4.getJdbcType();
      String var6 = null;
      if (null == var2) {
         if (JDBCType.UNKNOWN == var5) {
            var5 = JDBCType.CHAR;
         }

         this.setObject(var4, null, JavaType.OBJECT, var5, null, null, var3, var1, null);
      } else {
         JavaType var7 = JavaType.of(var2);
         if (JavaType.TVP == var7) {
            var6 = this.getTVPNameIfNull(var1, null);
         }

         var5 = var7.getJDBCType(SSType.UNKNOWN, var5);
         if (JDBCType.UNKNOWN == var5 && var2 instanceof UUID) {
            var7 = JavaType.STRING;
            var5 = JDBCType.GUID;
         }

         this.setObject(var4, var2, var7, var5, null, null, var3, var1, var6);
      }
   }

   public final void setObject(int var1, Object var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setObjectNoType(var1, var2, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, int var3) throws SQLServerException {
      String var4 = null;
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      if (-153 == var3) {
         var4 = this.getTVPNameIfNull(var1, null);
      }

      this.setObject(this.setterGetParam(var1), var2, JavaType.of(var2), JDBCType.of(var3), null, null, false, var1, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, SQLType var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber());
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setObject(
         this.setterGetParam(var1),
         var2,
         JavaType.of(var2),
         JDBCType.of(var3),
         2 != var3 && 3 != var3 && 93 != var3 && 92 != var3 && -155 != var3 && !InputStream.class.isInstance(var2) && !Reader.class.isInstance(var2)
            ? null
            : var4,
         null,
         false,
         var1,
         null
      );
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, int var3, Integer var4, int var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.setObject(
         this.setterGetParam(var1),
         var2,
         JavaType.of(var2),
         JDBCType.of(var3),
         2 != var3 && 3 != var3 && !InputStream.class.isInstance(var2) && !Reader.class.isInstance(var2) ? null : var5,
         var4,
         false,
         var1,
         null
      );
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, int var3, Integer var4, int var5, boolean var6) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5, var6});
      }

      this.checkClosed();
      this.setObject(
         this.setterGetParam(var1),
         var2,
         JavaType.of(var2),
         JDBCType.of(var3),
         2 != var3 && 3 != var3 && !InputStream.class.isInstance(var2) && !Reader.class.isInstance(var2) ? null : var5,
         var4,
         var6,
         var1,
         null
      );
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, SQLType var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber(), var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, SQLType var3, Integer var4, Integer var5) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber(), var4, var5.intValue(), false);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(int var1, Object var2, SQLType var3, Integer var4, Integer var5, boolean var6) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5, var6});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber(), var4, var5.intValue(), var6);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   final void setObject(Parameter var1, Object var2, JavaType var3, JDBCType var4, Integer var5, Integer var6, boolean var7, int var8, String var9) throws SQLServerException {
      assert JDBCType.UNKNOWN != var4;
      if (null == var2 && JavaType.TVP != var3) {
         assert JavaType.OBJECT == var3;
         if (var4.isUnsupported()) {
            var4 = JDBCType.BINARY;
         }

         var1.setValue(var4, null, JavaType.OBJECT, null, null, var6, var5, this.connection, false, this.stmtColumnEncriptionSetting, var8, this.userSQL, var9);
      } else {
         JDBCType var10 = var3.getJDBCType(SSType.UNKNOWN, var4);
         if (!var10.convertsTo(var4)) {
            DataTypes.throwConversionError(var10.toString(), var4.toString());
         }

         StreamSetterArgs var11 = null;
         switch (var3) {
            case READER:
               var11 = new StreamSetterArgs(StreamType.CHARACTER, -1L);
               break;
            case INPUTSTREAM:
               var11 = new StreamSetterArgs(var4.isTextual() ? StreamType.CHARACTER : StreamType.BINARY, -1L);
               break;
            case SQLXML:
               var11 = new StreamSetterArgs(StreamType.SQLXML, -1L);
         }

         var1.setValue(var4, var2, var3, var11, null, var6, var5, this.connection, var7, this.stmtColumnEncriptionSetting, var8, this.userSQL, var9);
      }
   }

   public final void setShort(int var1, short var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setShort", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLINT, var2, JavaType.SHORT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setShort");
   }

   public final void setShort(int var1, short var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setShort", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLINT, var2, JavaType.SHORT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setShort");
   }

   public final void setString(int var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.VARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setString");
   }

   public final void setString(int var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.VARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setString");
   }

   public final void setNString(int var1, String var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.NVARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setNString");
   }

   public final void setNString(int var1, String var2, boolean var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.NVARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNString");
   }

   public final void setTime(int var1, Time var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIME, var2, JavaType.TIME, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public final void setTime(int var1, Time var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIME, var2, JavaType.TIME, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public final void setTime(int var1, Time var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIME, var2, JavaType.TIME, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public final void setTimestamp(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public final void setTimestamp(int var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public final void setTimestamp(int var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public final void setDateTimeOffset(int var1, DateTimeOffset var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public final void setDateTimeOffset(int var1, DateTimeOffset var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public final void setDateTimeOffset(int var1, DateTimeOffset var2, int var3, boolean var4) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public final void setDate(int var1, Date var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATE, var2, JavaType.DATE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public final void setDateTime(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTime");
   }

   public final void setDateTime(int var1, Timestamp var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATETIME, var2, JavaType.TIMESTAMP, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTime");
   }

   public final void setSmallDateTime(int var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallDateTime");
   }

   public final void setSmallDateTime(int var1, Timestamp var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallDateTime");
   }

   public final void setStructured(int var1, String var2, SQLServerDataTable var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(var1, var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   public final void setStructured(int var1, String var2, ResultSet var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(var1, var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   public final void setStructured(int var1, String var2, ISQLServerDataRecord var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(var1, var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   String getTVPNameIfNull(int var1, String var2) throws SQLServerException {
      if ((null == var2 || 0 == var2.length()) && this instanceof SQLServerCallableStatement) {
         ParameterMetaData var3 = this.getParameterMetaData();

         try {
            var2 = var3.getParameterTypeName(var1);
         } catch (SQLException var5) {
            throw new SQLServerException(SQLServerException.getErrString("R_metaDataErrorForParameter"), null, 0, var5);
         }
      }

      return var2;
   }

   @Deprecated
   public final void setUnicodeStream(int var1, InputStream var2, int var3) throws SQLException {
      this.NotImplemented();
   }

   public final void addBatch() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "addBatch");
      this.checkClosed();
      if (this.batchParamValues == null) {
         this.batchParamValues = new ArrayList();
      }

      int var1 = this.inOutParam.length;
      Parameter[] var2 = new Parameter[var1];

      for (int var3 = 0; var3 < var1; var3++) {
         var2[var3] = this.inOutParam[var3].cloneForBatch();
      }

      this.batchParamValues.add(var2);
      loggerExternal.exiting(this.getClassNameLogging(), "addBatch");
   }

   public final void clearBatch() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "clearBatch");
      this.checkClosed();
      this.batchParamValues = null;
      loggerExternal.exiting(this.getClassNameLogging(), "clearBatch");
   }

   public int[] executeBatch() throws SQLServerException, BatchUpdateException {
      loggerExternal.entering(this.getClassNameLogging(), "executeBatch");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.discardLastExecutionResults();
      int[] var1;
      if (this.batchParamValues == null) {
         var1 = new int[0];
      } else {
         try {
            for (int var2 = 0; var2 < this.batchParamValues.size(); var2++) {
               Parameter[] var3 = (Parameter[])this.batchParamValues.get(var2);

               for (int var4 = 0; var4 < var3.length; var4++) {
                  if (var3[var4].isOutput()) {
                     throw new BatchUpdateException(SQLServerException.getErrString("R_outParamsNotPermittedinBatch"), null, 0, null);
                  }
               }
            }

            SQLServerPreparedStatement.PrepStmtBatchExecCmd var8 = new SQLServerPreparedStatement.PrepStmtBatchExecCmd(this);
            this.executeStatement(var8);
            var1 = new int[var8.updateCounts.length];

            for (int var9 = 0; var9 < var8.updateCounts.length; var9++) {
               var1[var9] = (int)var8.updateCounts[var9];
            }

            if (null != var8.batchException) {
               throw new BatchUpdateException(var8.batchException.getMessage(), var8.batchException.getSQLState(), var8.batchException.getErrorCode(), var1);
            }
         } finally {
            this.batchParamValues = null;
         }
      }

      loggerExternal.exiting(this.getClassNameLogging(), "executeBatch", var1);
      return var1;
   }

   public long[] executeLargeBatch() throws SQLServerException, BatchUpdateException {
      DriverJDBCVersion.checkSupportsJDBC42();
      loggerExternal.entering(this.getClassNameLogging(), "executeLargeBatch");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      this.discardLastExecutionResults();
      long[] var1;
      if (this.batchParamValues == null) {
         var1 = new long[0];
      } else {
         try {
            for (int var2 = 0; var2 < this.batchParamValues.size(); var2++) {
               Parameter[] var3 = (Parameter[])this.batchParamValues.get(var2);

               for (int var4 = 0; var4 < var3.length; var4++) {
                  if (var3[var4].isOutput()) {
                     throw new BatchUpdateException(SQLServerException.getErrString("R_outParamsNotPermittedinBatch"), null, 0, null);
                  }
               }
            }

            SQLServerPreparedStatement.PrepStmtBatchExecCmd var8 = new SQLServerPreparedStatement.PrepStmtBatchExecCmd(this);
            this.executeStatement(var8);
            var1 = new long[var8.updateCounts.length];

            for (int var9 = 0; var9 < var8.updateCounts.length; var9++) {
               var1[var9] = var8.updateCounts[var9];
            }

            if (null != var8.batchException) {
               DriverJDBCVersion.throwBatchUpdateException(var8.batchException, var1);
            }
         } finally {
            this.batchParamValues = null;
         }
      }

      loggerExternal.exiting(this.getClassNameLogging(), "executeLargeBatch", var1);
      return var1;
   }

   final void doExecutePreparedStatementBatch(SQLServerPreparedStatement.PrepStmtBatchExecCmd var1) throws SQLServerException {
      this.executeMethod = 4;
      var1.batchException = null;
      int var2 = this.batchParamValues.size();
      var1.updateCounts = new long[var2];

      for (int var3 = 0; var3 < var2; var3++) {
         var1.updateCounts[var3] = -3L;
      }

      int var12 = 0;
      int var4 = 0;
      Vector var5 = new Vector();
      if (this.isSelect(this.userSQL)) {
         SQLServerException.makeFromDriverError(this.connection, this, SQLServerException.getErrString("R_selectNotPermittedinBatch"), null, true);
      }

      this.connection.setMaxRows(0);
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      Parameter[] var6 = new Parameter[this.inOutParam.length];
      TDSWriter var7 = null;

      while (var4 < var2) {
         Parameter[] var8 = (Parameter[])this.batchParamValues.get(var12);
         assert var8.length == var6.length;

         for (int var9 = 0; var9 < var8.length; var9++) {
            var6[var9] = var8[var9];
         }

         boolean var13 = this.buildPreparedStrings(var6, false);
         if (0 == var4
            && Util.shouldHonorAEForParameters(this.stmtColumnEncriptionSetting, this.connection)
            && 0 < var6.length
            && !this.isInternalEncryptionQuery) {
            this.getParameterEncryptionMetadata(var6);
            this.buildPreparedStrings(var6, true);

            for (int var10 = 0; var10 < var6.length; var10++) {
               var5.add(var6[var10].cryptoMeta);
            }
         }

         if (0 < var4) {
            for (int var14 = 0; var14 < var5.size(); var14++) {
               var6[var14].cryptoMeta = (CryptoMetadata)var5.get(var14);
            }
         }

         if (var4 < var12) {
            var7.writeByte((byte)-1);
         } else {
            this.resetForReexecute();
            var7 = var1.startRequest((byte)3);
         }

         if (this.doPrepExec(var7, var6, var13) || ++var12 == var2) {
            this.ensureExecuteResultsReader(var1.startResponse(this.getIsResponseBufferingAdaptive()));

            while (var4 < var12) {
               this.startResults();

               try {
                  if (!this.getNextResult()) {
                     return;
                  }

                  if (null != this.resultSet) {
                     SQLServerException.makeFromDriverError(
                        this.connection, this, SQLServerException.getErrString("R_resultsetGeneratedForUpdate"), null, false
                     );
                  }
               } catch (SQLServerException var11) {
                  if (this.connection.isSessionUnAvailable() || this.connection.rolledBackTransaction()) {
                     throw var11;
                  }

                  this.updateCount = -3L;
                  if (null == var1.batchException) {
                     var1.batchException = var11;
                  }
               }

               var1.updateCounts[var4++] = -1L == this.updateCount ? -2L : this.updateCount;
               this.processBatch();
            }

            assert var4 == var12;
         }
      }
   }

   public final void setCharacterStream(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setCharacterStream(int var1, Reader var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setCharacterStream(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setNCharacterStream(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setNCharacterStream");
   }

   public final void setNCharacterStream(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNCharacterStream");
   }

   public final void setRef(int var1, Ref var2) throws SQLServerException {
      this.NotImplemented();
   }

   public final void setBlob(int var1, Blob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.BLOB, var2, JavaType.BLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public final void setBlob(int var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public final void setBlob(int var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public final void setClob(int var1, Clob var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.CLOB, var2, JavaType.CLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setClob(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setClob(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setNClob(int var1, NClob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.NCLOB, var2, JavaType.NCLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setNClob(int var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setNClob(int var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(var1, StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setArray(int var1, Array var2) throws SQLServerException {
      this.NotImplemented();
   }

   public final void setDate(int var1, Date var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATE, var2, JavaType.DATE, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public final void setDate(int var1, Date var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.DATE, var2, JavaType.DATE, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public final void setTime(int var1, Time var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIME, var2, JavaType.TIME, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public final void setTime(int var1, Time var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIME, var2, JavaType.TIME, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public final void setTimestamp(int var1, Timestamp var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public final void setTimestamp(int var1, Timestamp var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(var1, JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public final void setNull(int var1, int var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNull", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      if (-153 == var2) {
         this.setObject(this.setterGetParam(var1), null, JavaType.TVP, JDBCType.of(var2), null, null, false, var1, var3);
      } else {
         this.setObject(this.setterGetParam(var1), null, JavaType.OBJECT, JDBCType.of(var2), null, null, false, var1, var3);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "setNull");
   }

   public final ParameterMetaData getParameterMetaData() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getParameterMetaData");
      this.checkClosed();
      SQLServerParameterMetaData var1 = new SQLServerParameterMetaData(this, this.userSQL);
      loggerExternal.exiting(this.getClassNameLogging(), "getParameterMetaData", var1);
      return var1;
   }

   public final void setURL(int var1, URL var2) throws SQLServerException {
      this.NotImplemented();
   }

   public final void setRowId(int var1, RowId var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public final void setSQLXML(int var1, SQLXML var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSQLXML", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setSQLXMLInternal(var1, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setSQLXML");
   }

   public final int executeUpdate(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "executeUpdate", var1);
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_cannotTakeArgumentsPreparedOrCallable"));
      Object[] var3 = new Object[]{new String("executeUpdate()")};
      throw new SQLServerException(this, var2.format(var3), null, 0, false);
   }

   public final boolean execute(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "execute", var1);
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_cannotTakeArgumentsPreparedOrCallable"));
      Object[] var3 = new Object[]{new String("execute()")};
      throw new SQLServerException(this, var2.format(var3), null, 0, false);
   }

   public final ResultSet executeQuery(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "executeQuery", var1);
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_cannotTakeArgumentsPreparedOrCallable"));
      Object[] var3 = new Object[]{new String("executeQuery()")};
      throw new SQLServerException(this, var2.format(var3), null, 0, false);
   }

   public void addBatch(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "addBatch", var1);
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_cannotTakeArgumentsPreparedOrCallable"));
      Object[] var3 = new Object[]{new String("addBatch()")};
      throw new SQLServerException(this, var2.format(var3), null, 0, false);
   }

   private final class PrepStmtBatchExecCmd extends TDSCommand {
      private final SQLServerPreparedStatement stmt;
      SQLServerException batchException;
      long[] updateCounts;

      PrepStmtBatchExecCmd(SQLServerPreparedStatement var2) {
         super(var2.toString() + " executeBatch", SQLServerPreparedStatement.this.queryTimeout);
         this.stmt = var2;
      }

      final boolean doExecute() throws SQLServerException {
         this.stmt.doExecutePreparedStatementBatch(this);
         return true;
      }

      final void processResponse(TDSReader var1) throws SQLServerException {
         SQLServerPreparedStatement.this.ensureExecuteResultsReader(var1);
         SQLServerPreparedStatement.this.processExecuteResults();
      }
   }

   private final class PrepStmtExecCmd extends TDSCommand {
      private final SQLServerPreparedStatement stmt;

      PrepStmtExecCmd(SQLServerPreparedStatement var2, int var3) {
         super(var2.toString() + " executeXXX", SQLServerPreparedStatement.this.queryTimeout);
         this.stmt = var2;
         var2.executeMethod = var3;
      }

      final boolean doExecute() throws SQLServerException {
         this.stmt.doExecutePreparedStatement(this);
         return false;
      }

      final void processResponse(TDSReader var1) throws SQLServerException {
         SQLServerPreparedStatement.this.ensureExecuteResultsReader(var1);
         SQLServerPreparedStatement.this.processExecuteResults();
      }
   }
}
